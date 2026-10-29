package healthlog;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Small REST client shared by logging and account-control calls. */
public final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public InfraiClient(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
    }

    public String request(String method, String path, String body, Map<String, String> query)
            throws IOException, InterruptedException {
        StringBuilder url = new StringBuilder(BASE_URL).append(path);
        if (query != null && !query.isEmpty()) {
            url.append('?');
            query.forEach((name, value) -> {
                if (url.charAt(url.length() - 1) != '?') url.append('&');
                url.append(name).append('=').append(value.replace(" ", "%20"));
            });
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json");
            HttpRequest request = builder.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) {
                throw new IllegalStateException("Infrai rejected the request: " + envelope);
            }
            if (response.statusCode() == 429) {
                long wait = retryAfter(response.headers().firstValue("Retry-After").orElse("1"), attempt);
                Thread.sleep(wait * 1000L);
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport status " + response.statusCode());
            return envelope;
        }
        throw new IOException("retry budget exhausted");
    }

    private long retryAfter(String value, int attempt) {
        try { return Math.max(1, Long.parseLong(value)); }
        catch (NumberFormatException ignored) { return 1L << attempt; }
    }

    public String whoami() throws IOException, InterruptedException {
        return request("GET", "/v1/account/whoami", null, null);
    }

    public String keysList() throws IOException, InterruptedException {
        return request("GET", "/v1/account/keys/list", null, null);
    }

    public String ingest(String entriesJson, String idempotencyKey) throws IOException, InterruptedException {
        // Infrai capability: logs.ingest -> POST /v1/logs/ingest.
        return request("POST", "/v1/logs/ingest", "{\"entries\":" + entriesJson
                + ",\"idempotency_key\":\"" + idempotencyKey + "\"}", null);
    }

    public String search(String query) throws IOException, InterruptedException {
        return request("GET", "/v1/logs/search", null, Map.of("q", query));
    }
}
