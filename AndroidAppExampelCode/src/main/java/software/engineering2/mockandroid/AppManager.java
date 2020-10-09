package software.engineering2.mockandroid;

import android.os.Handler;
import android.util.Log;

import java.util.HashMap;

import software.engineering2.mockandroid.models.gadgets.GadgetType;
import software.engineering2.mockandroid.models.gadgets.Gadget_basic;
import software.engineering2.mockandroid.network.HTTPNetworkService;

public class AppManager {
    private static final String TAG = "Info";
    // Handler of UI-thread. For communication: Service threads -> UI thread
    private Handler handler;
    // Holds a reference to the fragment currently displayed
    public ResponseUpdatable currentFragmentView;
    // NetWork service
    private HTTPNetworkService httpNetworkService;

    // map for gadgets. GadgetID/object
    private HashMap<Integer, Gadget_basic> gadgets;

    public HashMap<Integer, Gadget_basic> getGadgets() {
        return gadgets;
    }

    private static AppManager instance = null;

    public static AppManager getInstance() {
        if (instance == null) {
            instance = new AppManager();
        }
        return instance;
    }

    private AppManager() {
        handler = new Handler();
        currentFragmentView = null;
        gadgets = new HashMap<>();

    }

    public void handleServerResponse(String response) {
        String[] commands = response.split("::");
        switch (commands[0]) {
            case "304":
                receiveAllGadgets(commands);
                break;
            case "316":
                gadgetStateUpdate(commands);
                break;
            case "901":
                Log.e(TAG, "Exception msg: " + commands[1]);
                break;
        }

    }

    // =========================== HANDLE NETWORK CONNECTION ===============================

    public void createServerConnection() {
        httpNetworkService = new HTTPNetworkService(handler);
    }

    public void closeServerConnection() {
        httpNetworkService.getWebSocketClient().close();
        Log.i(TAG, "C: Socket is closed!");
    }

    public void requestToServer(String request) {
        httpNetworkService.getWebSocketClient().send(request);
    }

    // =====================================================================================


    // #304
    private void receiveAllGadgets(String[] commands) {
        int nbrOfGadgets = Integer.parseInt(commands[1]);
        int count = 2; // Start index to read in gadgets
        for (int i = 0; i < nbrOfGadgets; i++) {
            int gadgetID = Integer.parseInt(commands[count++]);
            String alias = commands[count++];
            GadgetType type = GadgetType.valueOf(commands[count++]);
            String valueTemplate = commands[count++];
            float state = Float.parseFloat(commands[count++]);
            long pollDelaySeconds = Long.parseLong(commands[count++]);
            Gadget_basic gadgetBasic = new Gadget_basic(gadgetID, alias, type, valueTemplate, state, pollDelaySeconds);
            gadgets.put(gadgetID, gadgetBasic);
        }
        currentFragmentView.update(304, "");
    }

    // #316
    private void gadgetStateUpdate(String[] commands) {
        int gadgetID = Integer.parseInt(commands[1]);
        float newState = Float.parseFloat(commands[2]);
        // Set new state
        gadgets.get(gadgetID).setState(newState);
        currentFragmentView.update(316, String.valueOf(newState));
    }
}
