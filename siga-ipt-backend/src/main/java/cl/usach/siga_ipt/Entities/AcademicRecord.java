package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Academic history entry. Created when an academic period is closed.
 * Course code and name are copied (snapshot) so later changes to the course
 * do not alter the history.
 */
@Entity
@Table(name = "academic_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id")
    private AcademicPeriod period;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal finalGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseResult result;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private String courseName;
}
