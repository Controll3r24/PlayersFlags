package it.controll3r24.playersflag;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ricerca paese da IP, su un singolo thread (rispetta il limite di richieste/minuto). */
final class GeoLocator {

    private static final Pattern CC = Pattern.compile("\"countryCode\"\\s*:\\s*\"([A-Za-z]{2})\"");

    private final PlayersFlagPlugin plugin;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PlayersFlag-Geo");
        t.setDaemon(true);
        return t;
    });

    GeoLocator(PlayersFlagPlugin plugin) {
        this.plugin = plugin;
    }

    void lookup(String ip, Consumer<Optional<String>> callback) {
        String template = plugin.getConfig().getString("geolocation.provider-url", "");
        int timeout = Math.max(1, plugin.getConfig().getInt("geolocation.timeout-seconds", 5));
        long delay = Math.max(0, plugin.getConfig().getLong("geolocation.request-delay-ms", 1500));
        String cleanIp = ip.replaceAll("%.*$", "");

        executor.execute(() -> {
            Optional<String> result = Optional.empty();
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(template.replace("%ip%", cleanIp)))
                        .timeout(Duration.ofSeconds(timeout))
                        .header("User-Agent", "PlayersFlag/" + plugin.getPluginMeta().getVersion())
                        .GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                Matcher m = CC.matcher(resp.body());
                if (m.find()) {
                    result = Optional.of(m.group(1).toUpperCase());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "Geolocalizzazione fallita", e);
            }
            try {
                callback.accept(result);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Errore nel callback geolocalizzazione", e);
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    void shutdown() {
        executor.shutdownNow();
    }
}
