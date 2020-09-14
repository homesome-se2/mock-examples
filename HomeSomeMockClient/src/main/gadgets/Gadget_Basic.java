package main.gadgets;

public class Gadget_Basic extends Gadget {

    /**
     * Class representing interface to native HomeSome hardware (physical) gadgets of all GadgetTypes.
     * The gadgets interacted with via this class are commonly built upon Arduino based WiFi-modules.
     */

    public Gadget_Basic(int gadgetID, String alias, GadgetType type, String valueTemplate, long pollDelaySeconds) {
        super(gadgetID, alias, type, valueTemplate, pollDelaySeconds);
    }

    public Gadget_Basic(int gadgetID, String alias, GadgetType type, String valueTemplate, float state, long pollDelaySeconds) {
        super(gadgetID, alias, type, valueTemplate, state, pollDelaySeconds);
    }

    @Override
    public void poll() {
        // Implement
    }

    @Override
    public void alterState(float requestedState) throws Exception {
        // Implement
    }

    @Override
    protected String sendCommand(String command) throws Exception {
        // Implement
        return null;
    }

    @Override
    public void setState(float newState) {
        // Implement
        super.setState(newState);
    }
}
