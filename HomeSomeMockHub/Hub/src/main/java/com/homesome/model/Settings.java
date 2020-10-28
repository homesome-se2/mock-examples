package com.homesome.model;

public class Settings {
    // Logs
    public boolean debugMode;
    // Add gadgets
    public boolean enableAddGadgets;
    public int tcpPortAddGadgets;
    // Remote access
    public boolean enableRemoteAccess;
    public String hubAlias;
    public int hubID;
    public String hubPwd;
    public String publicServerURL;
    public String publicServerURL_localTest;

    public Settings(){}

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isEnableAddGadgets() {
        return enableAddGadgets;
    }

    public int getTcpPortAddGadgets() {
        return tcpPortAddGadgets;
    }

    public boolean isEnableRemoteAccess() {
        return enableRemoteAccess;
    }

    public String getHubAlias() {
        return hubAlias;
    }

    public int getHubID() {
        return hubID;
    }

    public String getHubPwd() {
        return hubPwd;
    }

    public String getPublicServerURL() {
        return publicServerURL;
    }

    public String getPublicServerURL_localTest() {
        return publicServerURL_localTest;
    }
}
