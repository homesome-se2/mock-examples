package software.engineering2.mockandroid.network;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import software.engineering2.mockandroid.AppManager;
import tech.gusavila92.websocketclient.WebSocketClient;

public class HTTPNetworkService {

    private WebSocketClient webSocketClient;
    public static final String TAG = "Info";

    public static final String SERVER_IP = "134.209.198.123"; //server IP address
    public static final int SERVER_PORT = 8084;
    private Handler handler;

    public WebSocketClient getWebSocketClient() {
        return webSocketClient;
    }

    public HTTPNetworkService(Handler han) {
        this.handler = han;
        URI uri;
        try {
            // Connect to server
            uri = new URI("ws://"+SERVER_IP+":"+SERVER_PORT+"/homesome");
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                Log.i(TAG, "C: Connected to Server!");
            }

            @Override
            public void onTextReceived(String message) {
                Log.i(TAG, "Message received: " + message);
                updateUIThread(message);

            }

            @Override
            public void onBinaryReceived(byte[] data) {

            }

            @Override
            public void onPingReceived(byte[] data) {

            }

            @Override
            public void onPongReceived(byte[] data) {

            }

            @Override
            public void onException(Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }

            @Override
            public void onCloseReceived() {
                Log.i(TAG, "S: Closed ");
            }
        };
        Log.i(TAG, "C: Connecting...");
        webSocketClient.connect();
    }

    private void updateUIThread(final String request) {
        // Update UI thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                AppManager.getInstance().handleServerResponse(request);
            }
        });
    }
}
