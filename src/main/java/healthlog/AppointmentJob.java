package healthlog;

import java.time.Instant;

/** Runnable job: decide a patient-safe notification, then ship an audit line. */
public final class AppointmentJob {
    private final InfraiClient client;

    public AppointmentJob(InfraiClient client) { this.client = client; }

    public String run(String appointmentId, String patientId, String state) throws Exception {
        AppointmentNotification notification = AppointmentNotification.decide(appointmentId, patientId, state);
        String entry = "[{\"service\":\"appointment-job\",\"level\":\"info\","
                + "\"environment\":\"production\",\"message\":\"" + notification.message()
                + "\",\"appointment_id\":\"" + notification.appointmentId()
                + "\",\"patient_id\":\"" + notification.patientId()
                + "\",\"occurred_at\":\"" + Instant.now() + "\"}]";
        return client.ingest(entry, "appointment-" + appointmentId + "-" + state);
    }
}
