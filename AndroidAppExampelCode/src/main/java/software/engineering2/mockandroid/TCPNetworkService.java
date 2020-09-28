package software.engineering2.mockandroid;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TCPNetworkService {

    public static final String TAG = "Info";

    public static final String SERVER_IP = "134.209.198.123"; //server IP address
    public static final int SERVER_PORT = 8084;
    private String mServerMessage;
    private boolean mRun = false;
    private PrintWriter mBufferOut;
    private BufferedReader mBufferIn;
    private Socket socket = null;
    private Handler handler;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPNetworkService(Handler han) {
        this.handler = han;
    }

    public void sendMessage(final String message) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.i(TAG, "Sending: " + message);
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        });
        thread.start();
    }

    public void stopClient() {
        mRun = false;
        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;

    }

    public void connect() {
        mRun = true;
        try {
            Log.i(TAG, "C: Connecting...");
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            socket = new Socket(serverAddr, SERVER_PORT);
            Log.i(TAG, "C: Connected!");
            mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (mRun) {
                mServerMessage = mBufferIn.readLine();
                if (mServerMessage != null) {
                    Log.i(TAG, "S: Received Message: '" + mServerMessage + "'");
                    updateUIThread(mServerMessage);
                }

            }
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            try {
                socket.close();
                Log.i(TAG, "C: Socket is closed: " + socket.isClosed());
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
        }
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