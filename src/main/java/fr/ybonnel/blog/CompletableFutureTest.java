package fr.ybonnel.blog;

import com.jayway.awaitility.Awaitility;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CompletableFutureTest {

    public static String getTitle(String url) {
        try {
            return Jsoup.connect(url).get().title();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String naiveMethod() throws IOException {
        String longestTitle = "";

        for (Element element : Jsoup.connect("http://www.ybonnel.fr/archive.html").get()
                .select("div.container>ul>li>a")) {
            String url = "http://www.ybonnel.fr/" + element.attr("href");

            String title = getTitle(url);

            if (title.length() > longestTitle.length()) {
                longestTitle = title;
            }
        }

        return longestTitle;
    }

    public static String parallelMethod() throws IOException, InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(20);

        List<Future<String>> results = new ArrayList<>();

        for (Element element : Jsoup.connect("http://www.ybonnel.fr/archive.html").get()
                .select("div.container>ul>li>a")) {

            String url = "http://www.ybonnel.fr/" + element.attr("href");

            // On ajoute à la liste des futures le résultat de la récupération du title.
            results.add(executor.submit(() -> getTitle(url)));
        }

        // On attend que tout les "Future" soient terminés.
        boolean allDone = false;
        while (!allDone) {
            allDone = true;
            for (Future<String> result : results) {
                if (!result.isDone()) {
                    allDone = false;
                    break;
                }
            }
            Thread.sleep(50);
        }

        // On récupère le titre le plus long.
        String longestTitle = "";
        for (Future<String> result : results) {
            if (result.get().length() > longestTitle.length()) {
                longestTitle = result.get();
            }
        }

        executor.shutdown();

        return longestTitle;
    }

    public static String betterMethod() throws ExecutionException, InterruptedException, IOException {
        ExecutorService executor = Executors.newFixedThreadPool(20);

        List<CompletableFuture<String>> titles = Jsoup.connect("http://www.ybonnel.fr/archive.html").get()
                .select("div.container>ul>li>a").stream()
                .map(element -> "http://www.ybonnel.fr/" + element.attr("href")) // Get URL
                .map(url -> CompletableFuture.supplyAsync(() -> getTitle(url), executor)) // Get Title
                .collect(Collectors.toList()); // ToList


        String longestTitle = CompletableFuture.allOf(titles.toArray(new CompletableFuture[titles.size()]))
                // apply something after all future has been completed.
                .thenApply((v) -> titles.stream()
                                // Get content of future (the title)
                                .map(cf -> cf.getNow(""))
                                // Get Max by length.
                                .max(Comparator.comparing(String::length)))
                .get() // Get content of future
                .get(); // Get content of optional.

        executor.shutdown();

        return longestTitle;
    }

    public static void chrono(String title, Callable<String> callable) throws Exception {
        long startTime = System.nanoTime();
        String result = callable.call();
        long elapsedTime = System.nanoTime() - startTime;
        System.out.println(title + " - " + TimeUnit.NANOSECONDS.toMillis(elapsedTime) + "ms");
        System.out.println("Longest title : " + result);
    }

    public static void main(String[] args) throws Exception {
        // Load all necessary classes before mesuring time.
        naiveMethod();
        parallelMethod();
        betterMethod();

        chrono("naive", CompletableFutureTest::naiveMethod);
        chrono("parallel", CompletableFutureTest::parallelMethod);
        chrono("better", CompletableFutureTest::betterMethod);
    }
}
