package healthlog;

public final class AppointmentNotificationTest {
    public static void main(String[] args) {
        AppointmentNotification result = AppointmentNotification.decide("apt-1", "patient-1", "confirmed");
        if (!result.message().equals("appointment confirmed")) throw new AssertionError(result);
        try {
            AppointmentNotification.decide("apt-1", "patient-1", "pending");
            throw new AssertionError("pending must not notify a patient");
        } catch (IllegalArgumentException expected) {
            System.out.println("appointment decision checks passed");
        }
    }
}
