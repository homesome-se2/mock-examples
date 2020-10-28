package com.homesome.model;

public class Gadget_Basic extends Gadget {

    private String ip;
    private int port;

    public Gadget_Basic(int gadgetID, String alias, GadgetType type, String valueTemplate, String requestSpec,
                        long pollDelaySeconds, String ip, int port) {
        super(gadgetID, alias, type, valueTemplate, requestSpec, pollDelaySeconds);
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void poll() {
        // Implement
    }

    @Override
    public void alterState(double requestedState) throws Exception {
        setState(requestedState);
    }

    @Override
    protected String sendCommand(String command) throws Exception {
        // Implement
        return null;
    }

    @Override
    public void setState(double newState) {
        // Implement
        super.setState(newState);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
