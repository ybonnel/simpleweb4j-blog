package fr.ybonnel.blog.websocket;

import fr.ybonnel.blog.websocket.WebSocket.Message;
import fr.ybonnel.simpleweb4j.handlers.RouteParameters;
import fr.ybonnel.simpleweb4j.handlers.websocket.SimpleWebSocketListener;
import fr.ybonnel.simpleweb4j.handlers.websocket.WebSocketSession;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.io.IOException;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.start;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.websocket;

public class WebSocket extends SimpleWebSocketListener<String, Message> {

    private final String user;
    private WebSocketSession<Message> session;

    public WebSocket(RouteParameters routeParameters) {
        super(String.class);
        this.user = routeParameters.getParam("name");
    }

    @Override public void onClose(int statusCode, String reason) {
        sessionOpenned.remove(session);
    }

    @Override public void onConnect(WebSocketSession<Message> session) {
        this.session = session;
        sessionOpenned.add(session);
    }

    @Override public void onMessage(String texte) {
        Message message = new Message(user, texte);
        sendMessageToAllSession(message);
    }

    private static void sendMessageToAllSession(Message message) {
        sessionOpenned.forEach(session -> sendMessageToOneSession(message, session));
    }

    private static void sendMessageToOneSession(Message message,
            WebSocketSession<Message> session) {
        try {
            session.sendMessage(message);
        } catch (IOException ignore) {
        }
    }

    private static ConcurrentHashSet<WebSocketSession<Message>> sessionOpenned = new ConcurrentHashSet<>();

    public static class Message {
        String user;
        String texte;

        public Message(String user, String texte) {
            this.user = user;
            this.texte = texte;
        }
    }

    public static void main(String[] args) {

        websocket("/chat/:name", WebSocket::new);

        start();
    }

}
