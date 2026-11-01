package cl.usach.siga_ipt.Exceptions;

/** Thrown on duplicates or when an operation conflicts with existing data (HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
