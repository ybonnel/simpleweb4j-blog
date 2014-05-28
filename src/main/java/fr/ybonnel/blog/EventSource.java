package fr.ybonnel.blog;

import fr.ybonnel.simpleweb4j.handlers.Response;
import fr.ybonnel.simpleweb4j.handlers.eventsource.EndOfStreamException;
import fr.ybonnel.simpleweb4j.handlers.eventsource.ReactiveHandler;
import fr.ybonnel.simpleweb4j.handlers.eventsource.ReactiveStream;
import fr.ybonnel.simpleweb4j.handlers.eventsource.Stream;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.get;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.start;

public class EventSource {

    private static ConcurrentHashSet<ReactiveHandler<String>> handlers;

    public static void main(String[] args) {

        get("/horloge", () -> new Response<Stream<String>>(
                Stream.newStream(() -> LocalTime.now().toString(), 1000)
        ));

        handlers = new ConcurrentHashSet<>();

        get("/reactivehorloge", (param, routeParam) -> new Response<ReactiveStream<String>>(
                handlers::add
        ));

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> handlers.forEach(EventSource::sendTimeToHandler),
                0, 1, TimeUnit.SECONDS
        );

        start();
    }

    private static void sendTimeToHandler(ReactiveHandler<String> handler) {
        try {
            handler.next(LocalTime.now().toString());
        } catch (EndOfStreamException e) {
            handlers.remove(handler);
        }
    }

}
