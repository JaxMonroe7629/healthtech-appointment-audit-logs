package healthlog;

public final class RunAppointmentJob {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        InfraiClient client = new InfraiClient(key);
        AppointmentJob job = new AppointmentJob(client);
        System.out.println(client.whoami());
        System.out.println(client.keysList());
        System.out.println(job.run("apt-1042", "patient-77", "confirmed"));
        System.out.println(client.search("appointment confirmed"));
    }
}
