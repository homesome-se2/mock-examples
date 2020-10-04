package model;

public class Client {

    public int sessionID;
    public static int sessionCounter = 0;
    public String name;
    public boolean loggedIn;

    public Client() {
        sessionID = ++sessionCounter;
        name = "Undefined";
        loggedIn = false;
    }
}
