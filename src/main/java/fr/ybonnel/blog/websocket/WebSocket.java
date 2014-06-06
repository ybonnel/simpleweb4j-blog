package fr.ybonnel.blog.websocket;

import fr.ybonnel.simpleweb4j.handlers.websocket.WebSocketListener;
import fr.ybonnel.simpleweb4j.handlers.websocket.WebSocketSession;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.io.IOException;
import java.util.Date;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.start;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.websocket;

public class WebSocket {

    private static ConcurrentHashSet<WebSocketSession<Message>> sessionOpenned = new ConcurrentHashSet<>();

    public static class Message {
        String user;
        String texte;
        Date date;

        public Message(String user, String texte) {
            this.user = user;
            this.texte = texte;
            this.date = new Date();
        }
    }

    public static void main(String[] args) {

        websocket("/chat/:name", (routeParameters) ->
                WebSocketListener.<String, Message>newBuilder(String.class)
                    .onConnect(sessionOpenned::add)
                    .onMessage((session, texte) ->
                            sendMessageToAllSession(new Message(routeParameters.getParam("name"), texte)))
                    .onClose((session, cause) -> sessionOpenned.remove(session))
                    .build()
        );

        start();
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

}
