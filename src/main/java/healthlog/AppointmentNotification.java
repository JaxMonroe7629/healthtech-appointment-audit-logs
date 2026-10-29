package healthlog;

public record AppointmentNotification(String appointmentId, String patientId, String state, String message) {
    public static AppointmentNotification decide(String appointmentId, String patientId, String state) {
        if (!state.equals("confirmed") && !state.equals("cancelled")) {
            throw new IllegalArgumentException("unsupported appointment state");
        }
        String message = state.equals("confirmed") ? "appointment confirmed" : "appointment cancelled";
        return new AppointmentNotification(appointmentId, patientId, state, message);
    }
}
