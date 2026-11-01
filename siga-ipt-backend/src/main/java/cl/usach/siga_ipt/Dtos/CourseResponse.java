package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.Course;

import java.util.Comparator;
import java.util.List;

public record CourseResponse(
        Long id,
        String code,
        String name,
        int semester,
        int theoryHours,
        int exerciseHours,
        int labHours,
        int sct,
        Long studyPlanId,
        List<Long> prerequisiteIds,
        List<String> prerequisiteCodes) {

    public static CourseResponse from(Course c) {
        List<Course> prereqs = c.getPrerequisites().stream()
                .sorted(Comparator.comparing(Course::getCode))
                .toList();
        return new CourseResponse(
                c.getId(), c.getCode(), c.getName(), c.getSemester(),
                c.getTheoryHours(), c.getExerciseHours(), c.getLabHours(), c.getSct(),
                c.getStudyPlan().getId(),
                prereqs.stream().map(Course::getId).toList(),
                prereqs.stream().map(Course::getCode).toList());
    }
}
