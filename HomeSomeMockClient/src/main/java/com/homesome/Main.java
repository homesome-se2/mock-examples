package com.homesome;

public class Main {

    public static void main(String[] args) {
	ClientApp client = new ClientApp();

        // Clean up in case of external shut down
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Shutdown hook running");
                client.closeApp();
            }
        }));

	client.launchApp();
    }
}
