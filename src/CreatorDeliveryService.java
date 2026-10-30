import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class CreatorDeliveryService {
    private final InfraiHttpClient client;
    private final String bucket;

    public CreatorDeliveryService(InfraiHttpClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public String fillAndArchive(CoursePacket packet, Path form) throws IOException, InterruptedException {
        String canonicalImport = "pdf.form.fill";
        String pdf = java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(form));
        String fields = "{\"course_title\":\"" + json(packet.courseTitle()) + "\",\"subscriber_email\":\""
                + json(packet.subscriberEmail()) + "\",\"lesson_count\":\"" + packet.lessonCount() + "\"}";
        String filled = client.post("/v1/pdf/form/fill", "{\"pdf\":\"" + pdf + "\",\"fields\":" + fields + ",\"flatten\":true}");
        String archiveKey = "courses/" + packet.subscriberEmail() + "/" + packet.courseSlug() + ".pdf";
        String resultData = extractData(filled);
        client.post("/v1/storage/bucket/create", "{\"name\":\"" + json(bucket) + "\"}");
        client.put("/v1/storage/object/put/" + bucket + "/" + archiveKey,
                "{\"data_base64\":\"" + resultData + "\",\"content_type\":\"application/pdf\",\"idempotency_key\":\""
                        + json(packet.deliveryId()) + "\"}");
        return archiveKey;
    }

    static boolean shouldSendUpdate(CoursePacket packet) {
        return packet.lessonCount() > 0 && !packet.subscriberEmail().isBlank();
    }

    private static String extractData(String envelope) {
        int marker = envelope.indexOf("\"data\":\"");
        if (marker < 0) throw new IllegalArgumentException("The fill response has no data payload");
        int start = marker + 8;
        int end = envelope.indexOf('"', start);
        if (end < 0) throw new IllegalArgumentException("The fill response data is incomplete");
        return envelope.substring(start, end);
    }

    private static String json(String value) {
        return Objects.requireNonNull(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record CoursePacket(String courseTitle, String courseSlug, String subscriberEmail, int lessonCount, String deliveryId) {}
}
