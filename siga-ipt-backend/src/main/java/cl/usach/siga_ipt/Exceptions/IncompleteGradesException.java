package cl.usach.siga_ipt.Exceptions;

import java.util.List;

/** Thrown when a period cannot be closed because some enrollments have no final grade (HTTP 422). */
public class IncompleteGradesException extends RuntimeException {

    private final List<String> details;

    public IncompleteGradesException(String message, List<String> details) {
        super(message);
        this.details = details;
    }

    /** One line per incomplete section. */
    public List<String> getDetails() {
        return details;
    }
}
