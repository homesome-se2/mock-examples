package com.homesome.model;

public class Client {

    // NEW:
    public int sessionID;
    public boolean loggedIn;
    private static int sessionCounter = 0;

    // Client is either a hub itself, or a user that should be mapped to a hub.
    public final int hubID;


    public Client() {
        loggedIn = false;
        sessionID = ++sessionCounter;
        hubID = -1;
    }

    public Client(int hubID) {
        loggedIn = false;
        sessionID = ++sessionCounter;
        this.hubID = hubID;
    }

}
