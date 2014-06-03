package fr.ybonnel.blog;

import fr.ybonnel.simpleweb4j.handlers.Response;

import static fr.ybonnel.simpleweb4j.SimpleWeb4j.get;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.start;

public class ExposeJson {

    private static class HelloObject {
        String prefixe;
        String suffixe;

        private HelloObject(String prefixe, String suffixe) {
            this.prefixe = prefixe;
            this.suffixe = suffixe;
        }
    }

    public static void main(String[] args) {
        get("/hello", () -> new Response<>("Hello world"));

        get("/hello/object", () ->
                new Response<>(new HelloObject("Hello", "World")));

        get("/hello/:name", (param, routeParams) ->
                new Response<>("Hello " + routeParams.getParam("name")));

        start();
    }
}
