package main.gadgets;

public abstract class Gadget {

    /**
     *
     * @param id Unique gadget id
     * @param alias User friendly name set by admin client
     * @param type What type of gadget this is, e.g. BINARY_SENSOR, SWITCH..
     * @param state Current state of the gadget
     * @param valueTemplate Reference to a register specifying how the gadget should be displayed at client end.
     * @param lastPollTime When the gadget was last polled (currentMillis)
     * @param pollDelaySec The uniquelly specified delay between polling the gadget statate (seconds)
     * @param isPresent States whether the Server could reach the gadget at last attempt (poll/state request).
     *                  true: Gadget is displayed for clients
     *                  false: Gadget is not displayed for clients
     *
     * @see #poll() Polls the gadget state and presence at interval: pollDelaySec
     * - Objective: set isPresent
     * - Objective: set state
     * @see #alterState(float) Sends request to gadget to alter its state (for non-sensor gadgets)
     * - Objective: communicate state change request to gadget
     * - Objective: set state
     * @see #sendCommand(String) Used by #poll() and #alterState() to communicate with the gadget
     * - Objective: communicate with gadget according to gadget architecture
     *
     */

    public final int id;
    public final String alias;
    public final GadgetType type;
    private float state;
    public String valueTemplate;
    public long lastPollTime;
    public final long pollDelaySec;
    public boolean isPresent;

    // Gadgets instantiated from JSON file (gadgets.json) at system boot??
    public Gadget(int gadgetID, String alias, GadgetType type, String valueTemplate, long pollDelaySeconds) {
        this.id = gadgetID;
        this.alias = alias;
        this.type = type;
        this.valueTemplate = valueTemplate;
        state = -1;
        pollDelaySec = pollDelaySeconds;
        isPresent = false;
    }

    public Gadget(int gadgetID, String alias, GadgetType type, String valueTemplate, float state, long pollDelaySeconds) {
        this.id = gadgetID;
        this.alias = alias;
        this.type = type;
        this.valueTemplate = valueTemplate;
        this.state = state;
        pollDelaySec = pollDelaySeconds;
        isPresent = false;
    }

    // Request gadget's current state
    public abstract void poll();

    // Request gadget to alter state
    public abstract void alterState(float requestedState) throws Exception;

    // Communications specifics for sending request to a gadget
    protected abstract String sendCommand(String command) throws Exception;

    // Set instance variable 'state' to match actual state (called when a gadget has reported a state change)
    public void setState(float newState) {
        boolean isBinaryGadget = (type == GadgetType.SWITCH || type == GadgetType.BINARY_SENSOR);
        if (isBinaryGadget) {
            state = (newState == 1 ? 1 : 0);
        } else {
            state = newState;
        }
    }

    public float getState() {
        return state;
    }

    // Translate gadget according to HoSo protocol. Gadget object -> HoSo protocol
    public String toHoSoProtocol() {
        return String.format("%s::%s::%s::%s::%s::%s", id, alias, type, valueTemplate, state, pollDelaySec);
    }

}