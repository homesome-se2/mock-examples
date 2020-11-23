package com.homesome.model;

public abstract class Gadget {

    public final int id;
    public final String alias;
    public final GadgetType type;
    private double state;
    public String valueTemplate;
    public String requestSpec;
    public final long pollDelaySec;
    public long lastPollTime;
    public boolean isPresent;



    public Gadget(int gadgetID, String alias, GadgetType type, String valueTemplate, String requestSpec, long pollDelaySeconds) {
        this.id = gadgetID;
        this.alias = alias;
        this.type = type;
        this.valueTemplate = valueTemplate;
        this.requestSpec = requestSpec;
        state = -1; // Will be set by poll()
        pollDelaySec = pollDelaySeconds;
        lastPollTime = 0;
        isPresent = false;
    }

    // Request gadget's current state
    public abstract void poll();

    // Request gadget to alter state
    public abstract void alterState(double requestedState) throws Exception;

    // Communications specifics for sending request to a gadget
    protected abstract String sendCommand(String command) throws Exception;

    // Set instance variable 'state' to match actual state (called when a gadget has reported a state change)
    public void setState(double newState) {
        boolean isBinaryGadget = (type == GadgetType.SWITCH || type == GadgetType.BINARY_SENSOR);
        boolean isSetValue = type == GadgetType.SET_VALUE;
        if (isBinaryGadget) {
            state = (newState == 1 ? 1 : 0);
        } else if(isSetValue) {
            //TODO: THIS IS SOMETHING THAT WOULD BE DONE IN THE GADGET IN A NOT MOCK IMPLEMENTATION
            newState = Math.round(newState); // Round to integer
            state = newState < 0 ? 0 : (newState > 100 ? 100 : newState); // Set it to an integer within span 0-100
        } else {
            state = newState;
        }
    }

    public double getState() {
        return state;
    }

    // Translate gadget according to HoSo protocol. Gadget object -> HoSo protocol
    public String toHoSoProtocol() {
        return String.format("%s::%s::%s::%s::%s::%s", id, alias, type, valueTemplate, state, pollDelaySec);
    }

}