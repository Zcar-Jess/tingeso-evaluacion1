package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "period_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id")
    private AcademicPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    /** 1..60 */
    @Column(nullable = false)
    private int capacity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "section_schedule_blocks",
            joinColumns = @JoinColumn(name = "section_id"))
    @Builder.Default
    private Set<ScheduleBlock> scheduleBlocks = new HashSet<>();
}
