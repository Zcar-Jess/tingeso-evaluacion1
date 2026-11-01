package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String run;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String professionalTitle;

    @Column(nullable = false)
    private String academicDegree;

    /** ACTIVE on creation. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherStatus status;
}
