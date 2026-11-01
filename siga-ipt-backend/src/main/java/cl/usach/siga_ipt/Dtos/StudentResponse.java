package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;

public record StudentResponse(
        Long id,
        String run,
        String fullName,
        String email,
        StudentStatus status,
        Long careerId,
        String careerName,
        Long studyPlanId,
        String studyPlanIdentifier) {

    public static StudentResponse from(Student s) {
        return new StudentResponse(
                s.getId(), s.getRun(), s.getFullName(), s.getEmail(), s.getStatus(),
                s.getCareer().getId(), s.getCareer().getName(),
                s.getStudyPlan().getId(), s.getStudyPlan().getIdentifier());
    }
}
