package ge.epam.gymcrm.exception;

/**
 * Thrown when the requested state change conflicts with the current state — for example
 * activating an already active profile (activate/de-activate is not idempotent).
 * Maps to 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
