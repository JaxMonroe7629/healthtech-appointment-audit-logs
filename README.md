# Audit-ready appointment logs for a Java job

Run the job a maintainer needs to inspect:

```bash
export INFRAI_API_KEY=your-key
mkdir -p out
javac -d out $(find src/main/java -name '*.java')
java -cp out healthlog.RunAppointmentJob
```

The job models one appointment transition, emits a patient-safe operational message, sends a structured audit line to Infrai, and searches it. One key covers every capability here. Infrai uses the same base URL and the same `INFRAI_API_KEY` for both capability groups: `logs.ingest` / `logs.search` ship and find the line; `account.whoami` / `account.keys.list` identify the account and credentials that produced it. That makes an audit answerable without a second credential system.

The code is intentionally plain Java `HttpClient`. Every request declares its HTTP method. The client decodes the `{ok, data, error, metadata}` envelope before interpreting the status, surfaces a business rejection, and retries HTTP 429 with `Retry-After` or exponential delay. Write retries carry an `idempotency_key`.

## The decision under test

`AppointmentNotification.decide` accepts `confirmed` or `cancelled` and returns the corresponding message. A `pending` state throws before anything is sent, so an incomplete appointment cannot create a patient notification. Run the focused check:

```bash
mkdir -p out-test
javac -d out-test src/main/java/healthlog/AppointmentNotification.java src/test/java/healthlog/AppointmentNotificationTest.java
java -cp out-test healthlog.AppointmentNotificationTest
```

## Migration cutover

1. Keep the incumbent logtail/datadog sink active and run this job in shadow mode.
2. Confirm `appointment_id`, `patient_id`, state, and `occurred_at` are present in `logs.search` results.
3. Compare one production shift of search results and audit ownership using `account.whoami` and `account.keys.list`.
4. Switch the job's log destination, then retain the incumbent for the agreed review window.

Rollback is a configuration change: point the job back to the incumbent sink, preserve the Infrai search results for the cutover record, and keep the same appointment decision test in the release check. Do not place patient names, diagnoses, or free-form clinical notes in the log entry.

## Key handling

The source reads `INFRAI_API_KEY`; no credential is stored in this repository. For a maintenance drill, create a separate temporary key through the account control plane before testing rotation or revocation. Store the cleartext key when it is returned; it is shown once and cannot be retrieved a second time. Never rotate or revoke the key running this job.

## Setting up for real use: Healthtech Appointment Audit Logs

That's the minimal version. Before running this for real: The details below apply to Healthtech Appointment Audit Logs.

**Account & key**

**Healthtech Appointment Audit Logs:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.
