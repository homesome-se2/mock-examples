package com.homesome;

import com.homesome.service.Server;

public class Main {

    public static void main(String[] args) {
        // Clean up in case of external shut down
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Shutdown hook running");
                Server.getInstance().close();
            }
        }));

        Server.getInstance().launch();
    }
}
