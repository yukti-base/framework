package org.yuktisetu.core.recovery;

/**
 * One piece of work a service picks back up after a crash or restart, before it
 * takes new requests: an eligibility run that was mid-way, outbox events that
 * gave up while Kafka was down, a drive whose scoring never started.
 *
 * Implementations must be idempotent -- recovery runs on every start, and on a
 * clean start it should find nothing to do. Order them with @Order when one
 * depends on another; each runs in its own try/catch, so one failing recovery is
 * logged and never keeps the service shut.
 */
public interface StartupRecovery {

    /** Short name for the logs: "outbox", "eligibility runs". */
    String name();

    /** @return how many items were picked back up, for the log line */
    int recover();
}
