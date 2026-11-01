package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"study_plan_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    /** 1..4 */
    @Column(nullable = false)
    private int semester;

    @Column(nullable = false)
    private int theoryHours;

    @Column(nullable = false)
    private int exerciseHours;

    @Column(nullable = false)
    private int labHours;

    /** 1..7 */
    @Column(nullable = false)
    private int sct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_plan_id")
    private StudyPlan studyPlan;

    @ManyToMany
    @JoinTable(name = "course_prerequisites",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id"))
    @Builder.Default
    private Set<Course> prerequisites = new HashSet<>();
}
