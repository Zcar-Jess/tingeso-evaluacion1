package cl.usach.siga_ipt.Config;

import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Entities.TeacherStatus;
import cl.usach.siga_ipt.Repositories.CareerRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Creates one teacher and one student whose emails match the demo users of the Keycloak realm
 * (docente@ipt.cl and estudiante@ipt.cl), so the login works right after importing the realm.
 * Only runs when {@code siga.demo-data=true} (environment variable DEMO_DATA).
 */
@Slf4j
@Component
@DependsOn("dataLoader")
@ConditionalOnProperty(name = "siga.demo-data", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataLoader implements CommandLineRunner {

    private static final String TEACHER_EMAIL = "docente@ipt.cl";
    private static final String STUDENT_EMAIL = "estudiante@ipt.cl";

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public void run(String... args) {
        if (!teacherRepository.existsByEmail(TEACHER_EMAIL) && !teacherRepository.existsByRun("90000001-1")) {
            teacherRepository.save(Teacher.builder()
                    .run("90000001-1")
                    .fullName("Docente Demo")
                    .email(TEACHER_EMAIL)
                    .professionalTitle("Ingeniero Civil Informático")
                    .academicDegree("Magíster")
                    .status(TeacherStatus.ACTIVE)
                    .build());
            log.info("Demo teacher created: {}", TEACHER_EMAIL);
        }

        if (!studentRepository.existsByEmail(STUDENT_EMAIL) && !studentRepository.existsByRun("90000002-2")) {
            Career career = careerRepository.findAll().stream()
                    .filter(c -> "10450".equals(c.getCode()))
                    .findFirst()
                    .orElse(null);
            StudyPlan plan = career == null ? null
                    : studyPlanRepository.findByCareerIdAndCurrentTrue(career.getId()).orElse(null);
            if (plan == null) {
                log.warn("Demo student not created: career 10450 or its current study plan was not found");
                return;
            }
            studentRepository.save(Student.builder()
                    .run("90000002-2")
                    .fullName("Estudiante Demo")
                    .email(STUDENT_EMAIL)
                    .status(StudentStatus.REGULAR)
                    .career(career)
                    .studyPlan(plan)
                    .build());
            log.info("Demo student created: {}", STUDENT_EMAIL);
        }
    }
}
