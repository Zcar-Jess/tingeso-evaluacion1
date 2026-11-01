package cl.usach.siga_ipt.Dtos;

/**
 * The logged-in user as SIGA IPT sees him. userId is the id of the linked Student or Teacher
 * (null for the administrator, who has no record in the database).
 */
public record MeResponse(
        String role,
        Long userId,
        String name,
        String email) {
}
