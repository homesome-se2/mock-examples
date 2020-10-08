package com.homesome;

import com.homesome.model.Gadget;
import com.homesome.model.GadgetType;
import com.homesome.model.Gadget_Basic;
import com.homesome.web_resources.ServerConnection;

import java.util.HashMap;
import java.util.Scanner;

public class ClientApp {

    private HashMap<Integer, Gadget> gadgets;
    private Thread outputThread;
    private final Object lockObject;
    private volatile boolean terminate;
    private final String info;

    public ClientApp() {
        gadgets = new HashMap<>();
        lockObject = new Object();
        terminate = false;
        info = String.format("%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n",
                "=================================================================================",
                "Connected to public server, but not yet logged in.",
                "Training simulator for:",
                "¤ Output to public server: Manual client login (any name/pwd works in simulation)",
                "¤ Input from public server: Receive all gadgets of the associated hub",
                "¤ Input from public server: Receive a state update on a gadget (at intervals)",
                "=================================================================================",
                "Start asynchronous communication:");
        outputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    outputToServer();
                } catch (Exception e) {
                    //Ignore
                } finally {
                    System.out.println("Output thread closed");
                    closeApp();
                }
            }
        });
    }

    public void launchApp() {
        try {
            // Print menu
            System.out.println(info);
            // Start WebSocket connection to server
            ServerConnection.getInstance().connectToServer();
            // Read client input to send to server
            outputThread.start();
            // Process incoming commands from server.
            inputFromServer();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            closeApp();
        }
    }

    public void closeApp() {
        synchronized (lockObject) {
            if (!terminate) {
                terminate = true;
                ServerConnection.getInstance().closeConnection();
            }
        }
    }

    // ========================= SERVER COMMUNICATION ====================================

    private void outputToServer() throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (!terminate) {
            // Type request from keyboard
            String hoSoRequest = scanner.nextLine();

            ServerConnection.getInstance().writeToServer(hoSoRequest);
        }
    }

    // ================= PROCESS SERVER COMMANDS ====================================

    private void inputFromServer() throws Exception {
        while (!terminate) {
            String commandFromServer = ServerConnection.getInstance().serverCommands.take();
            String[] commands = commandFromServer.split("::");

            switch (commands[0]) {
                case "102":
                    successfulManualLogin(commands);
                    break;
                case "304":
                    receiveAllGadgets(commands);
                    break;
                case "316":
                    gadgetStateUpdate(commands);
                    break;
                case "901":
                    System.out.println("Exception msg: " + commands[1]);
                    break;
                default:
                    System.out.println("Unknown msg from server: " + commandFromServer);
            }
        }
    }

    // 102
    private void successfulManualLogin(String[] commands) throws Exception {
        String name = commands[1];
        boolean admin = commands[2].equals("true");
        String homeAlias = commands[3];
        String sessionKey = commands[4];
        System.out.println(String.format("%s%n%-13s%s%n%-13s%s%n%-13s%s%n%-13s%s%n%s%n",
                "======= SUCCESSFUL LOGIN ========",
                "Name:", name,
                "Admin:", admin ? "Yes" : "No",
                "Home alias:", homeAlias,
                "sessionKey:", sessionKey,
                "================================="));
    }

    // #304
    private void receiveAllGadgets(String[] commands) throws Exception {
        int nbrOfGadgets = Integer.parseInt(commands[1]);
        int count = 2; // Start index to read in model
        for (int i = 0; i < nbrOfGadgets; i++) {
            int gadgetID = Integer.parseInt(commands[count++]);
            String alias = commands[count++];
            GadgetType type = GadgetType.valueOf(commands[count++]);
            String valueTemplate = commands[count++];
            float state = Float.parseFloat(commands[count++]);
            long pollDelaySeconds = Long.parseLong(commands[count++]);

            gadgets.put(gadgetID, new Gadget_Basic(gadgetID, alias, type, valueTemplate, state, pollDelaySeconds));
        }
        printGadgets();
    }

    // #316
    private void gadgetStateUpdate(String[] commands) throws Exception {
        int gadgetID = Integer.parseInt(commands[1]);
        float newState = Float.parseFloat(commands[2]);

        // Set new state
        gadgets.get(gadgetID).setState(newState);

        printGadgets();
    }

    private void printGadgets() {
        if (!gadgets.isEmpty()) {
            // Print all model
            System.out.println("========= ALL GADGETS ===========");
            for (int key : gadgets.keySet()) {
                System.out.println(String.format("%-13s: state %s", gadgets.get(key).alias, gadgets.get(key).getState()));
            }
            System.out.println("=================================");
        } else {
            System.out.println("Gadget list is empty.");
        }
    }

}
