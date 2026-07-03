package de.ottoextra.translateutils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Dünner asynchroner Client für LibreTranslate-kompatible APIs.
 * Kein Aufruf blockiert den aufrufenden Thread; Timeouts kommen aus der Config.
 */
public final class LibreTranslateClient {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private LibreTranslateClient() {
    }

    /**
     * Übersetzt {@code text} asynchron. Schlägt mit Exception fehl bei
     * Netzwerkfehler, Timeout, Nicht-200-Status oder unlesbarer Antwort.
     */
    public static CompletableFuture<String> translate(TranslateUtilsConfig config, String text) {
        JsonObject body = new JsonObject();
        body.addProperty("q", text);
        body.addProperty("source", config.sourceLanguage);
        body.addProperty("target", config.targetLanguage);
        body.addProperty("format", "text");
        if (!config.apiKey.isBlank()) {
            body.addProperty("api_key", config.apiKey);
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(config.apiUrl.trim()))
                    .timeout(Duration.ofMillis(config.timeoutMillis))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                    .build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("HTTP " + response.statusCode());
                    }
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
                    if (json == null || !json.has("translatedText")) {
                        throw new IllegalStateException("Antwort ohne 'translatedText'");
                    }
                    return json.get("translatedText").getAsString();
                });
    }
}
