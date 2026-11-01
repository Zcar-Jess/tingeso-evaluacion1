package cl.usach.siga_ipt.Exceptions;

/** Thrown when the caller is not allowed to perform an operation (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
