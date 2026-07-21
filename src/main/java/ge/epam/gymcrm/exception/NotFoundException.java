package ge.epam.gymcrm.exception;

/** Thrown when a requested profile, training or training type does not exist. Maps to 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
