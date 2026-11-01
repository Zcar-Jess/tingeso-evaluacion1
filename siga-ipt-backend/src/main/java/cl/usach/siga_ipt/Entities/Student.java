package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String run;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    /** REGULAR on creation. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Career career;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private StudyPlan studyPlan;
}
