package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Entities.TeacherStatus;

public record TeacherResponse(
        Long id,
        String run,
        String fullName,
        String email,
        String professionalTitle,
        String academicDegree,
        TeacherStatus status) {

    public static TeacherResponse from(Teacher t) {
        return new TeacherResponse(t.getId(), t.getRun(), t.getFullName(), t.getEmail(),
                t.getProfessionalTitle(), t.getAcademicDegree(), t.getStatus());
    }
}
