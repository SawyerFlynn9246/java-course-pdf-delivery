import java.nio.file.Path;

public final class CreatorDeliveryApp {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY before running the example");
        if (args.length != 4) throw new IllegalArgumentException("Usage: java CreatorDeliveryApp <form.pdf> <title> <email> <slug>");
        var packet = new CreatorDeliveryService.CoursePacket(args[1], args[3], args[2], 6, "delivery-" + args[3]);
        var service = new CreatorDeliveryService(new InfraiHttpClient(key), "course-deliveries");
        System.out.println("Archived flattened course PDF at " + service.fillAndArchive(packet, Path.of(args[0])));
    }
}
