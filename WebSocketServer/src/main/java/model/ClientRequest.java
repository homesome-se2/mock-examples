package model;

public class ClientRequest {

    public int sessionID;
    public String msg;

    public ClientRequest(int sessionID, String msg) {
        this.sessionID = sessionID;
        this.msg = msg;
    }
}
