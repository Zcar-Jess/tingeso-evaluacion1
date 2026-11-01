package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.DayCode;
import cl.usach.siga_ipt.Entities.ScheduleBlock;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** One schedule block: day (L, M, W, J, V) and module (1..6). Example: L2. */
public record ScheduleBlockDto(
        @NotNull DayCode day,
        @NotNull @Min(1) @Max(6) Integer module) {

    public ScheduleBlock toEntity() {
        return new ScheduleBlock(day, module);
    }

    public static ScheduleBlockDto from(ScheduleBlock b) {
        return new ScheduleBlockDto(b.getDay(), b.getModule());
    }

    public String code() {
        return day.name() + module;
    }
}
