package cl.usach.siga_ipt.Integration;

import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.AcademicRecord;
import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.CareerStatus;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.CourseResult;
import cl.usach.siga_ipt.Entities.DayCode;
import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.PeriodRegistration;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.ScheduleBlock;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Entities.TeacherStatus;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import cl.usach.siga_ipt.Repositories.AcademicRecordRepository;
import cl.usach.siga_ipt.Repositories.CareerRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.PeriodRegistrationRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base of the integration tests. They start the whole application (controllers, security, services,
 * repositories) on an in-memory H2 database and call the real endpoints with MockMvc. Requests
 * carry a JWT like the one Keycloak would issue (role + email claim), so the real security rules run.
 *
 * <p>Every test runs in a transaction that is rolled back at the end, so tests do not affect each
 * other. The fixture below is created before each test:
 * a career with a study plan of four courses (A1, A2, A3 of semester 1 and B1 of semester 2, which
 * requires A1), an OPEN period with one section per course (capacity 30, different schedule blocks),
 * two teachers and two students (only {@code student} is registered in the period).
 */
@SpringBootTest
@Transactional
abstract class AbstractIntegrationBase {

    protected static final String TEACHER_EMAIL = "teacher.it@ipt.cl";
    protected static final String OTHER_TEACHER_EMAIL = "other.teacher.it@ipt.cl";
    protected static final String STUDENT_EMAIL = "student.it@ipt.cl";
    protected static final String OTHER_STUDENT_EMAIL = "other.student.it@ipt.cl";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    protected CareerRepository careerRepository;
    @Autowired
    protected StudyPlanRepository studyPlanRepository;
    @Autowired
    protected CourseRepository courseRepository;
    @Autowired
    protected StudentRepository studentRepository;
    @Autowired
    protected TeacherRepository teacherRepository;
    @Autowired
    protected AcademicPeriodRepository periodRepository;
    @Autowired
    protected PeriodRegistrationRepository registrationRepository;
    @Autowired
    protected SectionRepository sectionRepository;
    @Autowired
    protected EnrollmentRepository enrollmentRepository;
    @Autowired
    protected AcademicRecordRepository academicRecordRepository;

    protected MockMvc mockMvc;

    protected Career career;
    protected StudyPlan plan;
    protected Course a1;
    protected Course a2;
    protected Course a3;
    protected Course b1;
    protected AcademicPeriod period;
    protected Teacher teacher;
    protected Teacher otherTeacher;
    protected Student student;
    protected Student otherStudent;
    protected Section sectionA1;
    protected Section sectionA2;
    protected Section sectionA3;
    protected Section sectionB1;

    @BeforeEach
    void createFixture() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        career = careerRepository.save(Career.builder().code("IT-CAREER").name("Integration career")
                .description("Career used by the integration tests").status(CareerStatus.ACTIVE).build());
        plan = studyPlanRepository.save(StudyPlan.builder().identifier("IT.1").current(true).career(career).build());

        a1 = course("IT101", "Course A1", 1, plan);
        a2 = course("IT102", "Course A2", 1, plan);
        a3 = course("IT103", "Course A3", 1, plan);
        b1 = course("IT201", "Course B1", 2, plan);
        b1.getPrerequisites().add(a1);
        b1 = courseRepository.save(b1);

        period = periodRepository.save(AcademicPeriod.builder().academicYear(2090).semester(1)
                .status(PeriodStatus.OPEN).build());

        teacher = teacher("90100001-1", "Teacher IT", TEACHER_EMAIL);
        otherTeacher = teacher("90100002-2", "Other Teacher IT", OTHER_TEACHER_EMAIL);
        student = student("91000001-1", "Student IT", STUDENT_EMAIL);
        otherStudent = student("91000002-2", "Other Student IT", OTHER_STUDENT_EMAIL);

        sectionA1 = section(a1, DayCode.L, 1);
        sectionA2 = section(a2, DayCode.M, 1);
        sectionA3 = section(a3, DayCode.W, 1);
        sectionB1 = section(b1, DayCode.J, 1);

        register(student);
    }

    // ------------------------------------------------------------------ fixture helpers

    private Course course(String code, String name, int semester, StudyPlan studyPlan) {
        return courseRepository.save(Course.builder().code(code).name(name).semester(semester)
                .theoryHours(2).exerciseHours(2).labHours(2).sct(4).studyPlan(studyPlan)
                .prerequisites(new HashSet<>()).build());
    }

    private Teacher teacher(String run, String name, String email) {
        return teacherRepository.save(Teacher.builder().run(run).fullName(name).email(email)
                .professionalTitle("Engineer").academicDegree("Master").status(TeacherStatus.ACTIVE).build());
    }

    private Student student(String run, String name, String email) {
        return studentRepository.save(Student.builder().run(run).fullName(name).email(email)
                .status(StudentStatus.REGULAR).career(career).studyPlan(plan).build());
    }

    private Section section(Course course, DayCode day, int module) {
        Set<ScheduleBlock> blocks = new HashSet<>();
        blocks.add(new ScheduleBlock(day, module));
        return sectionRepository.save(Section.builder().course(course).period(period).teacher(teacher)
                .capacity(30).scheduleBlocks(blocks).build());
    }

    protected void register(Student who) {
        registrationRepository.save(PeriodRegistration.builder().student(who).period(period).build());
    }

    protected Enrollment enroll(Student who, Section section, String grade) {
        return enrollmentRepository.save(Enrollment.builder().student(who).course(section.getCourse())
                .section(section).period(section.getPeriod()).enrollmentDate(LocalDate.of(2090, 3, 1))
                .finalGrade(grade == null ? null : new BigDecimal(grade)).build());
    }

    protected AcademicRecord record(Student who, Course course, AcademicPeriod inPeriod, String grade,
                                    CourseResult result) {
        return academicRecordRepository.save(AcademicRecord.builder().student(who).course(course)
                .period(inPeriod).finalGrade(new BigDecimal(grade)).result(result)
                .courseCode(course.getCode()).courseName(course.getName()).build());
    }

    /** A previous, already closed period, to hold older academic records. */
    protected AcademicPeriod previousPeriod() {
        return periodRepository.save(AcademicPeriod.builder().academicYear(2089).semester(2)
                .status(PeriodStatus.CLOSED).build());
    }

    // ------------------------------------------------------------------ authentication helpers

    private static RequestPostProcessor user(String role, String email) {
        return jwt().jwt(token -> token.claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    protected static RequestPostProcessor admin() {
        return user("ADMIN", "admin@ipt.cl");
    }

    protected static RequestPostProcessor teacherUser(String email) {
        return user("TEACHER", email);
    }

    protected static RequestPostProcessor studentUser(String email) {
        return user("STUDENT", email);
    }
}
