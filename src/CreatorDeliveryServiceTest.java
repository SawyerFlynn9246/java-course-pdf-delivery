public final class CreatorDeliveryServiceTest {
    public static void main(String[] args) {
        var active = new CreatorDeliveryService.CoursePacket("Java Basics", "java-basics", "learner@example.com", 6, "delivery-1");
        var empty = new CreatorDeliveryService.CoursePacket("Java Basics", "java-basics", "", 6, "delivery-2");
        if (!CreatorDeliveryService.shouldSendUpdate(active)) throw new AssertionError("active subscriber should receive an update");
        if (CreatorDeliveryService.shouldSendUpdate(empty)) throw new AssertionError("missing email should not receive an update");
        System.out.println("course delivery decision test passed");
    }
}
