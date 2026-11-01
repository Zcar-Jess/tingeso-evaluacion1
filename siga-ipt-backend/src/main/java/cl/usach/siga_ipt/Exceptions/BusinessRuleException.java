package cl.usach.siga_ipt.Exceptions;

/** Thrown when a business rule is violated (HTTP 422). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
