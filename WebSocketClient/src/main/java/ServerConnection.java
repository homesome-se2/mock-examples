import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ServerConnection {

    private BlockingQueue<String> serverCommands;
    private Session session;
    private volatile boolean connectedToServer;

    private final Object lockObject_output;
    private final Object lockObject_close;

    // Make Singleton
    private static ServerConnection instance = null;

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    private ServerConnection(){
        serverCommands = new ArrayBlockingQueue<>(10);
        session = null;
        connectedToServer = false;
        lockObject_output = new Object();
        lockObject_close = new Object();
    }

    public void connectToServer() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            String uri = "ws://localhost:8086/test";
            container.connectToServer(WebSocketClient.class, URI.create(uri)); // returns a WebSocket session object
            processServerCommands();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onServerConnect(Session session) {
        this.session = session;
        connectedToServer = true;
        // Test write to server
        writeToServer("My name");
    }

    public void closeConnection() {
        synchronized (lockObject_close) {
            connectedToServer = false;
            newCommandFromServer("exit");
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void writeToServer(String msg) {
        synchronized (lockObject_output) {
            System.out.println("Msg to server: " + msg);
            try {
                if (connectedToServer && session.isOpen()) {
                    session.getBasicRemote().sendText(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void newCommandFromServer(String msg) {
        try {
            serverCommands.put(msg);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    private void processServerCommands() {
        try {
            while (connectedToServer) {
                String command = serverCommands.take();
                System.out.println("Msg from server: " + command);

                if (command.equals("exit")) {
                    break;
                } else {
                    // Process request ...
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: Here or in WebSocket implementation class: Scheduler/timer to send a ping msg to server at intervals.
    // Server closes client connections after 1 min idle = Clients must break the idle state periodically.
}
