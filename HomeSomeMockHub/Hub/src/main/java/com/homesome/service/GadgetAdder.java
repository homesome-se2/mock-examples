package com.homesome.service;

import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class GadgetAdder {

    private int port;
    private volatile ServerSocket serverSocket;

    public GadgetAdder(int port) {
        this.port = port;
    }

    public void launch() {
        System.out.println("Gadget adder running on " + getServerIP());
        Thread listener = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listen();
                } catch (Exception e) {
                    close();
                }
            }
        });
        listener.start();
    }

    public void close() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            System.out.println("Gadget adder shutting down.");
        }
    }

    private void listen() throws Exception {
        BufferedWriter output;
        BufferedReader input;

        // Launch server
        serverSocket = new ServerSocket(port);

        while (!Hub.getInstance().terminate) {
            Socket clientConnection = null;

            try {
                // Receive client connection requests
                clientConnection = serverSocket.accept();

                // Obtain output & input streams
                output = new BufferedWriter(new OutputStreamWriter(clientConnection.getOutputStream()));
                input = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));

                String request = input.readLine();
                String commands[] = request.split("::");

                // Process requests
                switch (commands[0]) {
                    case "601":
                        // Request from Android to verify hub address in LAN
                        String response = String.format("%s::%s%n", "602", Hub.getInstance().settings.hubAlias);
                        output.write(response);
                        output.flush();
                        System.out.println("Hub is pinged");
                        break;
                    case "620":
                        // Request from gadget device to add gadget(s) to hub.
                        request = String.format("%s::%s", request, getClientIP(clientConnection)); // Append client IP
                        Hub.getInstance().requests.put(request);
                        output.write(String.format("%s%n", "621"));
                        output.flush();
                        System.out.println("Request to add gadget(s)");
                        break;
                    default:
                        System.out.println("Invalid msg from client");
                        break;
                }
            } catch (Exception e) {
                // Ignore
            } finally {
                if (clientConnection != null) {
                    clientConnection.close();
                }
            }
        }
    }

    private String getServerIP () {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "Unable to get IP";
        }
    }

    private String getClientIP(Socket socket) {
        return socket.getInetAddress().toString().substring(1); // IP-format "/X.X.X.X" to "X.X.X.X"
    }
}
