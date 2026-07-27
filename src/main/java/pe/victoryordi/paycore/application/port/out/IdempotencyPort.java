package pe.victoryordi.paycore.application.port.out;

import java.time.Duration;
import java.util.UUID;

public interface IdempotencyPort {

    Reservation reserve(String key, String fingerprint, Duration ttl);

    void complete(
            String key,
            String fingerprint,
            String leaseToken,
            UUID paymentId,
            Duration ttl);

    void release(String key, String fingerprint, String leaseToken);

    enum ReservationStatus {
        ACQUIRED,
        REPLAY,
        CONFLICT,
        IN_PROGRESS
    }

    record Reservation(ReservationStatus status, UUID paymentId, String leaseToken) {

        public static Reservation acquired(String leaseToken) {
            return new Reservation(ReservationStatus.ACQUIRED, null, leaseToken);
        }

        public static Reservation replay(UUID paymentId) {
            return new Reservation(ReservationStatus.REPLAY, paymentId, null);
        }

        public static Reservation conflict() {
            return new Reservation(ReservationStatus.CONFLICT, null, null);
        }

        public static Reservation inProgress() {
            return new Reservation(ReservationStatus.IN_PROGRESS, null, null);
        }
    }
}
