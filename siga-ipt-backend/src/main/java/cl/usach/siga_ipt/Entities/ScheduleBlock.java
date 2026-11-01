package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

/** A schedule block such as L2 (Monday, module 2). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ScheduleBlock {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_code", nullable = false)
    private DayCode day;

    /** 1..6 */
    @Column(name = "module_number", nullable = false)
    private int module;
}
