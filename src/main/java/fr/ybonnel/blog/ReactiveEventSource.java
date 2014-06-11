package fr.ybonnel.blog;

import fr.ybonnel.simpleweb4j.handlers.Response;
import fr.ybonnel.simpleweb4j.handlers.eventsource.EndOfStreamException;
import fr.ybonnel.simpleweb4j.handlers.eventsource.ReactiveHandler;
import fr.ybonnel.simpleweb4j.handlers.eventsource.ReactiveStream;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.get;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.start;

public class ReactiveEventSource {

    // Un événement
    static class Event {
        String name;
        int value;
        String time;

        public Event(String name) {
            this.name = name;
            this.value = ThreadLocalRandom.current().nextInt(100);
            time = LocalTime.now().toString();
        }
    }

    // Boucle infinie générant des événements.
    static class EventGenerator implements Runnable {
        private String name;
        public EventGenerator(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            while (true) {
                // Création d'un nouvel événement
                Event event = new Event(name);
                // Pour chaque handler, on envoie l'événement
                handlers.forEach(handler -> sendEventToHandler(handler, event));
                try {
                    // Attente entre 0 et 20 secondes.
                    Thread.sleep(
                            ThreadLocalRandom.current().nextInt(
                                    (int) TimeUnit.SECONDS.toMillis(20)));
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    // Envoi d'un événement à un handler.
    static void sendEventToHandler(ReactiveHandler<Event> handler, Event event) {
        try {
            handler.next(event);
        } catch (EndOfStreamException e) {
            // En cas de fermeture du flux par le client, on supprimer le handler.
            handlers.remove(handler);
        }
    }

    private static Set<ReactiveHandler<Event>> handlers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void main(String[] args) {

        // Démarrage des générateurs.
        new Thread(new EventGenerator("Paris")).start();
        new Thread(new EventGenerator("Rennes")).start();
        new Thread(new EventGenerator("Niort")).start();

        // Déclaration de la route SimpleWeb4j
        // C'est le fait que la réponse contienne un ReactiveStream qui
        // va nous permettre de faire du Server Sent Events.
        // ReactiveStream est une interface avec une seule méthode
        // qui va être appelée avec le handler SimpleWeb4j lors de
        // l'ouverture du flux, on peut donc utiliser une lambda.
        get("/reactive", (param, routeParam) -> new Response<ReactiveStream<Event>>(
                handlers::add
        ));

        start();
    }
}
