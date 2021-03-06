package com.homesome.service;

import com.homesome.model.ClientRequest;
import com.homesome.model.Settings;
import com.homesome.temp_mock.Mock_Interaction;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Server {

    public BlockingQueue<ClientRequest> clientRequests;
    public volatile Settings settings;
    public volatile boolean terminateServer;

    // Lock objects
    private final Object lock_closeServer;
    private final Object lock_debugLogs;

    // Make Singleton
    private static Server instance = null;

    // Temporary mock
    private Mock_Interaction mock; // REMOVE LATER

    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    private Server() {
        clientRequests = new ArrayBlockingQueue<>(10);
        terminateServer = false;
        mock = new Mock_Interaction();
        lock_closeServer = new Object();
        lock_debugLogs = new Object();
    }

    public void launch() {
        System.out.println("HomeSome server running...");
        try {
            // Read in settings from JSON
            settings = new Settings();
            settings.readInSettings();
            mock.launch();

            // Launch ClientHandler
            ClientHandler.getInstance().launchWebSocketServer(settings.getServerPort(), settings.getClientLimit());

            processRequests();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            close();
        }
    }

    public void close() {
        synchronized (lock_closeServer) {
            if (!terminateServer) {
                terminateServer = true;
                mock.close();
                ClientHandler.getInstance().stopWebSocketServer();
                System.out.println("HomeSome server shutting down");
            }
        }
    }

    //================================ PROCESS CLIENT REQUESTS ==============================================

    // Executed by worker thread: processRequestsThread
    private void processRequests() throws Exception {
        while (!terminateServer) {
            try {
                ClientRequest clientRequest = clientRequests.take();
                String commands[] = clientRequest.request.split("::");
                int sessionID = clientRequest.sessionID;

                switch (commands[0]) {
                    case "105":
                        clientLogout(commands, sessionID);
                    case "302":
                        requestAllHubGadgets(commands, sessionID);
                        break;
                    case "303":
                        receiveAllHubGadgets(commands, sessionID);
                        break;
                    case "311":
                        requestGadgetStateChange(commands, sessionID);
                        break;
                    case "315":
                        receiveGadgetStateChange(commands, sessionID);
                        break;
                    case "370":
                        requestGadgetGroups(sessionID);
                        break;
                    case "372":
                        receiveGadgetGroups(commands);
                        break;
                    default:
                        String msg = "901::Invalid format";
                        ClientHandler.getInstance().outputToClients(sessionID, false, true, false, msg);
                        break;
                }
            } catch (InterruptedException e) {
                throw new Exception("Terminating processRequests()");
            } catch (Exception e) {
                // Ignore & carry on.
            }
        }
    }


    //TODO: Implement methods for all supported requests, according to HoSo protocol.

    /**
     * Notation above methods for forwarding scheme:
     *
     * #303 -> #304  : The incoming request is a #303 request, and the forwarding request should be a #304 command.
     * #302 -> #302  : The forwarding is the same command (#302 -> #302)
     * #105 -> X     : No forwarding to be done.
     */

    // #105 -> X
    private void clientLogout(String[] commands, int issuingSessionID) {
        //TODO: Implement
        // User client (Android/browser) has manually pressed the logout button.
        // Remove/overwrite the client's sessionKey in DB. This would force a manual login next time client wants to connect.
        // This method returns nothing (possibly just an exception msg '901::xxxx' if something goes wrong).
    }


    // #302 -> #302
    private void requestAllHubGadgets(String[] commands, int issuingSessionID) throws Exception {
        //TODO: Implement (and remove mock)
        // This method will be used when a newly logged in user requests all gadget data from its associated hub.
        // It is actually called from ClientHandler, but the request goes to the hub of which the newly logged in client belongs.
        // The msg is forwarded to the target hub as it is (no inspection or appending needed).

        mock.hubReportsAllGadgets(issuingSessionID); // REMOVE LATER
    }

    // #303 -> #304
    private void receiveAllHubGadgets(String[] commands, int issuingSessionID) throws Exception {
        int targetSessionID = Integer.parseInt(commands[1]);

        // Encapsulate (build) new command from the decapsulated incoming command (according to protocol)
        String forwardGadgetsMsg = "304";
        for (int command = 2 ; command < commands.length ; command++) {
            forwardGadgetsMsg = String.format("%s::%s", forwardGadgetsMsg, commands[command]);
        }
        // Send to individual client
        ClientHandler.getInstance().outputToClients(targetSessionID,false, true, false, forwardGadgetsMsg);
    }

    // #311 -> #312
    private void requestGadgetStateChange(String[] commands, int issuingSessionID) throws Exception {
        //TODO: Implement
        // Client requests to alter a gadget state.
        // Rebuild and forward the request to target hub as: #312
        // Look in ClientManager for appropriate method to use to locate the hub sessionID based on Client's hubID.
        // Note that the client's thread ID should be included in the forwarded msg to the hub.
    }

    // #315 -> #316
    private void receiveGadgetStateChange(String[] commands, int issuingSessionID) throws Exception {
        // Rebuild and forward the update to all users associated with that hub
        String gadgetID = commands[1];
        String newState = commands[2];
        String forwardMsg = String.format("%s::%s::%s", "316", gadgetID, newState);
        // Send to all users associated with that hub
        ClientHandler.getInstance().outputToClients(issuingSessionID, false, false, false, forwardMsg);
    }

    // #370 -> #371
    private void requestGadgetGroups(int issuingSessionID) {
        String forwardRequest = String.format("%s::%s", "371", issuingSessionID);
        // Send request to same hub as client with sessionID "issuingSessionID is associated with.
        //TODO: Implement

        mock.requestGadgetGroups(issuingSessionID); //TODO: REMOVE LATER
    }

    // #372 -> #373
    private void receiveGadgetGroups(String[] commands) throws Exception {
        int targetSessionID = Integer.parseInt(commands[1]);
        String forwardGroups = "373";
        // Encapsulate (build) new command from the decapsulated incoming command (according to protocol)
        for (int command = 2 ; command < commands.length ; command++) {
            forwardGroups = String.format("%s::%s", forwardGroups, commands[command]);
        }
        // Send to individual client
        ClientHandler.getInstance().outputToClients(targetSessionID,false, true, false, forwardGroups);
    }

    // ===================================== DEBUG LOGS =======================================================

    public void debugLog(String log, String... data) {
        synchronized (lock_debugLogs) {
            if(settings.isDebugMode()) {
                String logData = "";
                if(data.length > 0) {
                    logData = String.format("[%s]", data[0]);
                    if(data.length > 1 ) {
                        for(int i = 1 ; i < data.length; i++) {
                            logData = String.format("%s %s", logData, data[i]);
                        }
                    }
                }
                log = String.format("%-30s%s", log.concat(":"), logData);
                System.out.println(log.length() > 90 ? log.substring(0, 90).concat("[...]") : log);
            }
        }
    }

    public void debugLog(String log, int threadID, String... data) {
        synchronized (lock_debugLogs) {
            if(settings.isDebugMode()) {
                String logData = "";
                if(data.length > 0) {
                    logData = String.format("[%s] (Session %s)", data[0], threadID);
                    if(data.length > 1 ) {
                        for(int i = 1 ; i < data.length; i++) {
                            logData = String.format("%s %s", logData, data[i]);
                        }
                    }
                }
                log = String.format("%-30s%s", log.concat(":"), logData);
                System.out.println(log.length() > 90 ? log.substring(0, 90).concat("[...]") : log);
            }
        }
    }


}
