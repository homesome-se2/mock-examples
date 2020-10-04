package software.engineering2.mockandroid.models.gadgets;

public class Gadget_basic extends Gadget {

    public Gadget_basic(int gadgetID, String alias, GadgetType type, String valueTemplate, float state, long pollDelaySeconds) {
        super(gadgetID, alias, type, valueTemplate, state, pollDelaySeconds);
    }

    @Override
    public void poll() {

    }

    @Override
    public void alterState(float requestedState) throws Exception {

    }

    @Override
    protected String sendCommand(String command) throws Exception {
        return null;
    }
}
