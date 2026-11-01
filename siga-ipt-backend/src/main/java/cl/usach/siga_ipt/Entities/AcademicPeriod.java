package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "academic_periods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"academic_year", "semester"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false)
    private int academicYear;

    /** 1 or 2 */
    @Column(nullable = false)
    private int semester;

    /** OPEN on creation. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodStatus status;

    /** e.g. "2027-1" */
    @Transient
    public String getLabel() {
        return academicYear + "-" + semester;
    }
}
