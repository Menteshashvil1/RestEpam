package ge.epam.gymcrm.exception;

/** Thrown when the request is syntactically valid but semantically wrong. Maps to 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
