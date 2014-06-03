package fr.ybonnel.blog;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventSourceJetty extends AbstractHandler {

    public static void main(String[] args) throws Exception {
        // Déclaration est démarrage du serveur.
        Server jettyServer = new Server(9999);
        jettyServer.setHandler(new EventSourceJetty());
        jettyServer.start();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {

        // Content type pour du server-sent-events
        response.setContentType("text/event-stream");

        // On utilise un Continuation infini pour garder le flux ouvert
        Continuation continuation = ContinuationSupport.getContinuation(request);
        continuation.setTimeout(0L);
        continuation.suspend(response);

        // On utilise un ScheduledExecutorService pour exécuter un événement toutes les secondes.
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(() -> {
            try {
                PrintWriter writer = continuation.getServletResponse().getWriter();
                // Préfixe.
                writer.print("data: ");
                // Date courante.
                writer.print(LocalTime.now().toString());
                // Les deux retours à la ligne pour séparer les événements.
                writer.print("\n\n");
                // On flux le buffer.
                writer.flush();
                continuation.getServletResponse().flushBuffer();
            } catch (IOException ioException) {
                // Si on a une exception c'est que le flux a été fermé pas le client.
                // On arrête la Continuation et on arrête le ScheduledExecutorService.
                continuation.complete();
                service.shutdownNow();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
