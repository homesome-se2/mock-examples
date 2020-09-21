package main;

import main.gadgets.Gadget;
import main.gadgets.GadgetType;
import main.gadgets.Gadget_Basic;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class Client {

    private HashMap<Integer, Gadget> gadgets;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread outputThread;
    private final Object lockComm;
    private volatile boolean terminate;
    private final String info;

    public Client() {
        gadgets = new HashMap<>();
        socket = null;
        lockComm = new Object();
        terminate = false;
        info = String.format("%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n",
                "=================================================================================",
                "Connected to public server, but not yet logged in.",
                "Training simulator for:",
                "1. Output to public server: Manual client login (any name/pwd works in simulation)",
                "2. Input from public server: Receive all gadgets of the associated hub",
                "3. Input from public server: Receive a state update on a gadget (at intervals)",
                "=================================================================================",
                "Start asynchronous communication:");
        outputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    outputToServer();
                } catch (Exception e) {
                    System.out.println("output thread closed");
                    close();
                }
            }
        });
    }

    public void launch() {
        try {
            serverCommunication();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ========================= SERVER COMMUNICATION ====================================

    private void serverCommunication() throws Exception {
        try {
            // Request connection to server
            socket = new Socket("134.209.198.123", 8084);
            //socket = new Socket("localhost", 8084);
            System.out.println(info);

            // Obtain input and output streams
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            outputThread.start();

            inputFromServer();

        } catch (IOException e) {
            throw new Exception("Socket connection failed");
        } finally {
            close();
        }
    }

    private void inputFromServer() throws Exception {
        String msgFromServer;
        while (!terminate) {
            // Read msg from client
            msgFromServer = input.readLine();
            System.out.println("Msg from server: " + msgFromServer);
            // Process msg
            if(msgFromServer.equals("exit")) {
                close();
            } else {
                processServerResponse(msgFromServer);
            }
        }
    }

    private void outputToServer() throws Exception {
        Scanner scanner = new Scanner(System.in);
        while(!terminate) {
            // Type request from keyboard
            output.println(scanner.nextLine());
            output.flush();
            System.out.println("msg sent");
        }
    }

    // ================= PROCESS SERVER RESPONSE ====================================

    private void processServerResponse(String response) throws Exception {

        String[] commands = response.split("::");

        switch (commands[0]) {
            case "304":
                receiveAllGadgets(commands);
                break;
            case "316":
                gadgetStateUpdate(commands);
                break;
            case "901":
                System.out.println("Exception msg: " + commands[1]);
                break;
        }
    }

    // #304
    private void receiveAllGadgets(String[] commands) throws Exception {
        int nbrOfGadgets = Integer.parseInt(commands[1]);
        int count = 2; // Start index to read in gadgets
        for(int i = 0 ; i < nbrOfGadgets ; i++) {
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
        if(!gadgets.isEmpty()) {
            // Print all gadgets
            System.out.println("========= ALL GADGETS ===========");
            for (int key : gadgets.keySet()) {
                System.out.println(String.format("%-13s: state %s", gadgets.get(key).alias, gadgets.get(key).getState()));
            }
            System.out.println("=================================");
        }else {
            System.out.println("Gadget list is empty.");
        }
    }

    // ============================ CLOSE RESOURCES ====================================
    // Ugly close method

    private void close() {
        synchronized (lockComm) {
            if(!terminate) {
                terminate = true;
                System.out.println("closing threads");
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    if (outputThread.isAlive()) {
                        System.out.println("output still alive");
                    }
                    System.exit(-1);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Ignore
                }
            }
        }
    }

}
