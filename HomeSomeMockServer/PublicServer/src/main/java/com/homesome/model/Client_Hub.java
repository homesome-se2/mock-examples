package com.homesome.model;

public class Client_Hub extends Client {

    /**
     * Hubs connecting to the public server
     *
     * Needs to be separable from User clients,
     * e.g. to verify that a User (Android/Browser client) does not
     * attempt to make a request only permitted by
     */

    public String alias;

    public Client_Hub(int hubID, String alias) {
        super(hubID);
        this.alias = alias;
        loggedIn = true; // Client is logged in when specialized.
    }
}
