package com.homesome.service;

import com.google.gson.Gson;
import com.homesome.model.*;
import com.homesome.web_resources.ServerConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Hub {

    // Resources
    private GadgetAdder gadgetAdder;
    public BlockingQueue<String> requests;
    private HashMap<Integer, Gadget> gadgets;
    private ArrayList<GadgetGroup> gadgetGroups;;

    //Settings
    public volatile Settings settings;

    // Utilities
    private final Object lock_gadgets;
    private final Object lock_debugLog;
    public volatile boolean terminate;

    // config.json
    //Note: 'config.json' should be located "next to" the project folder: [config.json][PublicServer]
    private static final String configFileJSON = "./config.json";  // When run as JAR on Linux
    //private static final String configFileJSON = (new File(System.getProperty("user.dir")).getParentFile().getPath()).concat("/config.json"); // When run from IDE

    // Mock work thread
    private Thread mockPollThread;

    // Make Singleton
    private static Hub instance = null;

    public static Hub getInstance() {
        if (instance == null) {
            instance = new Hub();
        }
        return instance;
    }

    private Hub() {
        settings = null;
        gadgetAdder = null;
        requests = new ArrayBlockingQueue<>(10);
        gadgets = new HashMap<>();
        gadgetGroups = new ArrayList<>();
        lock_gadgets = new Object();
        lock_debugLog = new Object();
        terminate = false;
        createFakeGadgets();
        createFakeGroups();
        mockPollThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    launchFakePollResults();
                }catch (Exception e) {
                    System.out.println("Fake poll terminated");
                }
            }
        });
    }

    public void launchHub() {
        try {
            readInSettings();
            System.out.println("Local hub running...");
            if (settings.enableRemoteAccess) {
                String loginRequest = String.format("120::%s::%s::%s", settings.hubID, settings.hubPwd, settings.hubAlias );
                ServerConnection.getInstance().connectToServer(loginRequest);
            }
            if (settings.enableAddGadgets) {
                gadgetAdder = new GadgetAdder(settings.tcpPortAddGadgets);
                gadgetAdder.launch();
            }
            mockPollThread.start();
            try {
                processRequests();
            } catch (Exception e) {
                // Close
            } finally {
                close();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage()); // Unable to read config.json
        }
    }

    private void readInSettings() throws Exception{
        try (FileReader reader = new FileReader(configFileJSON)){
            settings = new Gson().fromJson(reader, Settings.class);
        } catch (FileNotFoundException e) {
            throw new Exception("Unable to read settings from config.json");
        }
    }

   /* private void readInGadgetGroups() throws Exception{
        try (FileReader reader = new FileReader(gadgetGroupsJsonPath)){
            gadgetGroups = new Gson().fromJson(reader, GadgetGroup[].class);
        } catch (FileNotFoundException e) {
            throw new Exception("Unable to read gadget groups from gadgetGroups.json");
        }
    }*/

    private void close() {
        if (!terminate) {
            terminate = true;
            if (settings.enableRemoteAccess) {
                ServerConnection.getInstance().closeConnection();
            }
            if (settings.enableAddGadgets) {
                gadgetAdder.close();
            }
            mockPollThread.interrupt();
            try {
                requests.put("exit");
            } catch (InterruptedException e) {
                //Ignore
            }
        }
    }

    // ======================================= PROCESS REQUESTS ========================================================

    private void processRequests() throws Exception {
        while (!terminate) {
            String[] request = requests.take().split("::");
            try {
                switch (request[0]) {
                    case "121":
                        successfulLogin();
                        break;
                    case "302":
                        requestAllGadgets(request);
                        break;
                    case "312":
                        requestToAlterGadgetState(request);
                        break;
                    case "371":
                        requestGadgetGroups(request);
                        break;
                    case "620":
                        addGadgets(request);
                        break;
                    case "901":
                        exception(request);
                        break;
                    case "exit":
                        return;
                    default:
                        break;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    // #121
    private void successfulLogin() {
        System.out.println("Login was successful");
        ServerConnection.getInstance().loggedInToServer = true;
    }

    // #302
    private void requestAllGadgets(String[] request) throws Exception {
        synchronized (lock_gadgets) {
            String response = String.format("303::%s::%s", request[1], gadgets.size());
            for (int key : gadgets.keySet()) {
                response = String.format("%s::%s", response, gadgets.get(key).toHoSoProtocol());
            }
            ServerConnection.getInstance().writeToServer(response);
        }
    }

    // #312
    private void requestToAlterGadgetState(String[] request) throws Exception {
        int gadgetID = Integer.parseInt(request[1]);
        double requestedState = Double.parseDouble(request[2]);
        synchronized (lock_gadgets) {
            Gadget gadget = gadgets.get(gadgetID);
            double currentState = gadget.getState();
            if (gadget.type == GadgetType.SWITCH || gadget.type == GadgetType.SET_VALUE) {
                gadget.setState(requestedState);
                currentState = gadget.getState();
            }
            String response = String.format("315::%s::%s", gadgetID, currentState);
            ServerConnection.getInstance().writeToServer(response);
            //TODO: Respond to PS even if state has not been altered
            // Otherwise client GUI may be locked in a hold where gadget is neither on/off (for example)
        }
    }

    // #371
    private void requestGadgetGroups(String[] request) throws Exception {
        synchronized (lock_gadgets) {
            String issuingUserSession = request[1];
            String response = "";
            if (gadgetGroups.size() > 0) {
                response = String.format("372::%s", issuingUserSession);
                for (GadgetGroup group : gadgetGroups) {
                    response = String.format("%s::%s", response, group.toHoSoProtocol());
                }
            } else {
                response = String.format("902::%s::%s", issuingUserSession, "No groups at hub");

            }
            ServerConnection.getInstance().writeToServer(response);
        }
    }

    // #620
    private void addGadgets(String[] request) {
        try {
            String gadgetIp = request[request.length-1]; // Appended by class GadgetAdder.
            String unitMac = request[1];
            if(!gadgetAlreadyAdded(unitMac)) {
                int gadgetPort = Integer.parseInt(request[2]);
                int nbrOfGadgets = Integer.parseInt(request[3]);
                int count = 3;
                for (int i = 0; i < nbrOfGadgets; i++) {
                    String alias = request[++count];
                    GadgetType type = GadgetType.valueOf(request[++count]);
                    String requestSpec = request[++count];
                    // Generate the rest of the values:
                    int gadgetID = generateGadgetID();
                    String valueTemplate = "default";
                    long pollDelaySeconds = 30;
                    Gadget_Basic newGadget = new Gadget_Basic(gadgetID, alias, type, valueTemplate, requestSpec, pollDelaySeconds, gadgetIp, gadgetPort, unitMac);
                    synchronized (lock_gadgets) {
                        gadgets.put(gadgetID, newGadget);
                        //TODO: New gadget should NOT be sent to PS here. Instead that should be done by poll, when isPresent == true.
                        //TODO: = Notify public server of new gadget(s) (#351): This is automatically done when gadget is discovered in poll()
                        //TODO: In hub: Constructor should set gadgets to: isPresent = false, lastPoll = 0 (to force poll on next poll iteration).
                        String output = String.format("351::%s", newGadget.toHoSoProtocol());
                        ServerConnection.getInstance().writeToServer(output);
                    }
                    //TODO: Add new gadget to gadgets.json
                }
            }
        } catch (Exception e) {
            System.out.println("Error on reading in new gadgets.");
            // Cancel operation
        }
        printGadgets();
    }

    // #901
    private void exception(String[] request) {
        System.out.println("Exception: " + request[1]);
    }

    // ========================================== UTILITIES ============================================================

    // Used when new gadgets are added via GadgetAdder.
    // If client has gadgets with IDs: [1, 2, 152] -> The new gadget will get ID 153.
    private int generateGadgetID() {
        synchronized (lock_gadgets) {
            int newID = 0;
            for (int gadgetID : gadgets.keySet()) {
                if (gadgetID >= newID) {
                    newID = gadgetID + 1;
                }
            }
            return newID;
        }
    }

    // Used when adding gadgets (plug & play) to verify that a gadget do not already exist in hub.
    private boolean gadgetAlreadyAdded(String unitMac) {
        synchronized (lock_gadgets) {
            for (int key : gadgets.keySet()) {
                Gadget gadget = gadgets.get(key);
                if (gadget instanceof Gadget_Basic) {
                    if(((Gadget_Basic) gadget).getUnitMac().equals(unitMac)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void createFakeGadgets() {
        synchronized (lock_gadgets) {
            gadgets.put(1, new Gadget_Basic(1, "My Lamp", GadgetType.SWITCH, "light", "null", 30, "192.168.0.23", 8084, "5C:CF:7F:0B:98:D8"));
            gadgets.put(2, new Gadget_Basic(2, "Window Lamp", GadgetType.SWITCH, "light", "null", 60, "192.168.0.24", 8084,"00:0a:95:9d:68:16"));
            gadgets.put(3, new Gadget_Basic(3, "Ad-blocker", GadgetType.SWITCH, "default", "null", 3600, "192.168.0.25", 8084, "00:A0:C9:14:C8:29"));
            gadgets.put(4, new Gadget_Basic(4, "Front door", GadgetType.BINARY_SENSOR, "door", "null", 2, "192.168.0.26", 8084, "00:00:0A:BB:28:FC"));
            gadgets.put(5, new Gadget_Basic(5, "Temperature", GadgetType.SENSOR, "temp", "temp", 120, "192.168.0.27", 8084, "00:1B:44:11:3A:B7"));
            gadgets.put(6, new Gadget_Basic(6, "TV volume", GadgetType.SET_VALUE, "default", "null", 3600, "192.168.0.28", 8084, "1B:0C:74:3F:3A:7F"));
            gadgets.get(1).setState(1);
            gadgets.get(2).setState(1);
            gadgets.get(3).setState(1);
            gadgets.get(4).setState(1);
            gadgets.get(5).setState(21.7);
            gadgets.get(6).setState(57);
        }
        printGadgets();
    }

    private void createFakeGroups() {
        gadgetGroups.add(new GadgetGroup("All lamps", new int[]{1, 2}));
        gadgetGroups.add(new GadgetGroup("All gadgets", new int[]{1, 2, 3, 4 ,5, 6}));
        gadgetGroups.add(new GadgetGroup("Kitchen", new int[]{2, 5}));
        gadgetGroups.add(new GadgetGroup("IT", new int[]{3}));
    }

    private void launchFakePollResults() throws Exception {
        int count = 0;
        while(!terminate) {
            Thread.sleep(30 * 1000);
            synchronized (lock_gadgets) {
                count++;
                // Toggle SWITCH gadget state
                gadgets.get(1).alterState(gadgets.get(1).getState() == 1 ? 0 : 1);
                String request_A = String.format("315::1::%s", gadgets.get(1).getState());
                ServerConnection.getInstance().writeToServer(request_A);
                if(count == 4) {
                    count = 0;
                    // Toggle SENSOR gadget state
                    gadgets.get(5).alterState(gadgets.get(5).getState() == 21.7 ? 22.1 : 21.7);
                    String request_B = String.format("315::5::%s", gadgets.get(5).getState());
                    ServerConnection.getInstance().writeToServer(request_B);
                }
            }
        }
    }

    private void printGadgets() {
        System.out.println(String.format("%s%n%-8s%-18s%-16s%-19s%-17s%-10s%-16s%s%n%s",
                line(), "ID", "ALIAS", "TYPE", "VALUE TEMPLATE", "IP", "PORT", "REQUEST SPEC", "MAC", line()));
        synchronized (lock_gadgets) {
            for (int gadgetID : gadgets.keySet()) {
                Gadget gadget = gadgets.get(gadgetID);
                System.out.println(String.format("%-8s%-18s%-16s%-19s%-17s%-10s%-16s%s",
                        gadget.id, gadget.alias, gadget.type.toString(), gadget.valueTemplate, ((Gadget_Basic)gadget).getIp(), ((Gadget_Basic)gadget).getPort(), gadget.requestSpec, ((Gadget_Basic)gadget).getUnitMac()));
            }
        }
        System.out.println(line());
    }

    private String line() {
        return "=========================================================================================================================";
    }

    public void debugLog(String log) {
        synchronized (lock_debugLog) {
            if(settings.debugMode) {
                System.out.println(log);
            }
        }
    }
}
