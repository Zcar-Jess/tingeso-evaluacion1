package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.Section;

import java.util.Comparator;
import java.util.List;

public record SectionResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseName,
        Long periodId,
        String period,
        Long teacherId,
        String teacherName,
        int capacity,
        long enrolled,
        long availableSeats,
        List<String> schedule,
        List<ScheduleBlockDto> scheduleBlocks) {

    public static SectionResponse from(Section s, long enrolled) {
        List<ScheduleBlockDto> blocks = s.getScheduleBlocks().stream()
                .map(ScheduleBlockDto::from)
                .sorted(Comparator.comparing(ScheduleBlockDto::day).thenComparing(ScheduleBlockDto::module))
                .toList();
        return new SectionResponse(
                s.getId(),
                s.getCourse().getId(), s.getCourse().getCode(), s.getCourse().getName(),
                s.getPeriod().getId(), s.getPeriod().getLabel(),
                s.getTeacher().getId(), s.getTeacher().getFullName(),
                s.getCapacity(), enrolled, s.getCapacity() - enrolled,
                blocks.stream().map(ScheduleBlockDto::code).toList(),
                blocks);
    }
}
