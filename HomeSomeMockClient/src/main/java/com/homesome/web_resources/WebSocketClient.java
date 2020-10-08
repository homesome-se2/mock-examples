package com.homesome.web_resources;

import javax.websocket.*;

@ClientEndpoint
public class WebSocketClient {

    @OnOpen
    public void connected(Session session) {
        ServerConnection.getInstance().onServerConnect(session);
    }

    @OnMessage
    public void newMessage(String msg) {
        ServerConnection.getInstance().newCommandFromServer(msg);
    }

    @OnClose
    public void disconnected(Session session, CloseReason reason) {
        ServerConnection.getInstance().onServerClose();
    }

}
