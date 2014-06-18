package fr.ybonnel.blog.websocket;

import fr.ybonnel.simpleweb4j.handlers.ContentType;
import org.assertj.core.groups.Tuple;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.jayway.awaitility.Awaitility.await;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

public class WebSocketTest {

    /** port choisi au hasard */
    private static int port = ThreadLocalRandom.current().nextInt(20000, 30000);
    /** client websocket */
    private WebSocketClient client;

    /**
     * Démarrage du serveur et du client websocket.
     */
    @Before
    public void setup() throws Exception {
        websocket("/chat/:name", WebSocket::buildListenner);
        setPort(port);
        start(false);

        client = new WebSocketClient();
        client.start();
    }

    /**
     * Arrêt du serveur et du client websocket.
     */
    @After
    public void stopServer() throws Exception {
        client.stop();

        stop();
    }

    /**
     * Ouverture d'une session websocket qui envoi les messages à un Consumer.
     */
    private Session openSession(
            Consumer<Message> consumer,
            String name) throws Exception {
        return client.connect(
                new WebSocketAdapter() {
                    @Override
                    public void onWebSocketText(String message) {
                        consumer.accept(ContentType.GSON.fromJson(
                                message, Message.class));
                    }
                },
                new URI("ws://localhost:" + port + "/chat/" + name)
        ).get();
    }

    @Test
    public void testWebSocket() throws Exception {
        // Listes permettant de stocker les messages reçus par les sessions.
        List<Message> messagesReceivedForSession1 =
                Collections.synchronizedList(new ArrayList<>());
        List<Message> messagesReceivedForSession2 =
                Collections.synchronizedList(new ArrayList<>());

        // Ouverture de la session pour le client1
        Session session1 = openSession(messagesReceivedForSession1::add, "client1");

        // Le client1 se sent seul (message 1).
        session1.getRemote().sendString("\"alone\"");
        // On attend d'avoir reçu la réponse du serveur.
        await().until(messagesReceivedForSession1::size, equalTo(1));

        // Connexion du client2
        Session session2 = openSession(messagesReceivedForSession2::add, "client2");

        // Echange de politesses entre les deux clients (message 2 et 3).
        session2.getRemote().sendString("\"hello client1\"");
        session1.getRemote().sendString("\"hello client2\"");

        // On attend d'avoir tout reçu côté client1
        await().until(messagesReceivedForSession1::size, equalTo(3));
        // Déconnexion du client1
        session1.disconnect();

        // Le client2 dit aurevoir (message 4)
        session2.getRemote().sendString("\"good bye\"");

        // Le client2 se déconnecte après avoir reçu la réponse du serveur.
        await().until(messagesReceivedForSession2::size, equalTo(3));
        session2.disconnect();

        // Vérification des messages reçus par le client1 (messages 1 à 3).
        assertThat(messagesReceivedForSession1).extracting("user", "texte")
                .containsExactly(
                        Tuple.tuple("client1", "alone"),
                        Tuple.tuple("client2", "hello client1"),
                        Tuple.tuple("client1", "hello client2")
                );

        // Vérification des messages reçus par le client2 (message 2 à 4).
        assertThat(messagesReceivedForSession2).extracting("user", "texte")
                .containsExactly(
                        Tuple.tuple("client2", "hello client1"),
                        Tuple.tuple("client1", "hello client2"),
                        Tuple.tuple("client2", "good bye")
                );
    }
}
