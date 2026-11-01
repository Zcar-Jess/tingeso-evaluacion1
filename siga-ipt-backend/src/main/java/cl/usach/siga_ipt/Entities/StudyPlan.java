package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"career_id", "identifier"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "2021.3" */
    @Column(nullable = false)
    private String identifier;

    /** Only one current plan per career. */
    @Column(name = "is_current", nullable = false)
    private boolean current;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "career_id")
    private Career career;
}
