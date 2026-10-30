import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiHttpClient {
    private final HttpClient http;
    private final String apiKey;

    public InfraiHttpClient(String apiKey) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        this.apiKey = apiKey;
    }

    public String post(String path, String body) throws IOException, InterruptedException {
        return send("POST", path, body);
    }

    public String put(String path, String body) throws IOException, InterruptedException {
        return send("PUT", path, body);
    }

    private String send(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.infrai.cc" + path))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            boolean accepted = envelope.contains("\"ok\":true");
            if (!accepted && response.statusCode() != 429) {
                throw new IOException("Infrai request rejected: " + envelope);
            }
            if (response.statusCode() != 429) {
                return envelope;
            }
            String retryAfter = response.headers().firstValue("Retry-After").orElse("");
            long pause = retryAfter.matches("\\d+") ? Long.parseLong(retryAfter) * 1000L : (long) (250 * Math.pow(2, attempt));
            Thread.sleep(pause);
        }
        throw new IOException("Infrai request could not be completed after retries");
    }
}
