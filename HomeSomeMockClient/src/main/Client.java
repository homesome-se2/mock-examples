package main;

import main.gadgets.Gadget;
import main.gadgets.GadgetType;
import main.gadgets.Gadget_Basic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class Client {

    private HashMap<Integer, Gadget> gadgets;

    public Client() {
        gadgets = new HashMap<>();
    }

    public void launch() {
        String requestAllGadgets = "301";
        String requestGadgetStateChange = "303::1::1";

        try {
            sendServerRequest(requestAllGadgets);
            sendServerRequest(requestGadgetStateChange);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // ========================= SERVER COMMUNICATION ====================================

    private void sendServerRequest(String request) throws Exception {
        Socket socket = null;
        DataInputStream input = null;
        DataOutputStream output = null;
        try {
            // Request connection to server
            socket = new Socket("134.209.198.123", 8083);

            // Obtain input and output streams
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            // Send request
            output.writeUTF(request);
            output.flush();

            // Read response
            String response = input.readUTF();

            // Process response
            processServerResponse(response);

        } catch (IOException e) {
            throw new Exception("Socket connection failed");
        } finally {
            // Closing the stream resource closes the Socket.
            if( input != null) {
                input.close();
            }
            if( output != null) {
                output.close();
            }
        }
    }

    // ================= PROCESS SERVER RESPONSE ====================================

    private void processServerResponse(String response) throws Exception {

        String[] commands = response.split("::");

        System.out.println("Msg from server: " + response);

        switch (commands[0]) {
            case "302":
                receiveAllGadgets(commands);
                break;
            case "304":
                alterGadgetState(commands);
                break;
            case "501":
                break;
        }

        if(!gadgets.isEmpty()) {
            // Print all gadgets
            for (int key : gadgets.keySet()) {
                System.out.println(String.format("%-10s: state %s", gadgets.get(key).alias, gadgets.get(key).getState()));
            }
        }else {
            System.out.println("Gadget list is empty.");
        }
    }

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
    }

    private void alterGadgetState(String[] commands) throws Exception {
        int gadgetID = Integer.parseInt(commands[1]);
        float requestedState = Float.parseFloat(commands[2]);

        System.out.println("new state = " + requestedState);

        // Set new state
        gadgets.get(gadgetID).setState(requestedState);
    }


}
