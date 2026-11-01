package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

/** Records that a student is enrolled (matriculated) in an academic period. */
@Entity
@Table(name = "period_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "period_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id")
    private AcademicPeriod period;
}
