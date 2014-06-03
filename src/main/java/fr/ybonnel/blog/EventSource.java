package fr.ybonnel.blog;

import fr.ybonnel.simpleweb4j.handlers.Response;
import fr.ybonnel.simpleweb4j.handlers.eventsource.Stream;

import java.time.LocalTime;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.get;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.start;

public class EventSource {

    public static void main(String[] args) {
        get("/horloge", () -> new Response<>(
                Stream.newStream(() -> LocalTime.now().toString(), 1000)
        ));

        start();
    }
}
