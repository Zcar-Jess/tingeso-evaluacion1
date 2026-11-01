package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.AcademicRecordResponse;
import cl.usach.siga_ipt.Dtos.StudentRequest;
import cl.usach.siga_ipt.Dtos.StudentResponse;
import cl.usach.siga_ipt.Dtos.StudentUpdateRequest;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.AcademicRecord;
import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.CareerStatus;
import cl.usach.siga_ipt.Entities.CourseResult;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicRecordRepository;
import cl.usach.siga_ipt.Repositories.CareerRepository;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private CareerRepository careerRepository;
    @Mock private StudyPlanRepository studyPlanRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private AcademicRecordRepository academicRecordRepository;
    @InjectMocks private StudentService service;

    private Career career1;
    private Career career2;
    private StudyPlan plan1;
    private StudyPlan plan2;

    @BeforeEach
    void setUp() {
        career1 = career(1L, "10450", CareerStatus.ACTIVE);
        career2 = career(2L, "10451", CareerStatus.ACTIVE);
        plan1 = plan(5L, career1, true);
        plan2 = plan(6L, career2, true);
    }

    // ------------------------------------------------------------------ helpers

    private Career career(Long id, String code, CareerStatus status) {
        return Career.builder().id(id).code(code).name("Career " + code)
                .description("desc").status(status).build();
    }

    private StudyPlan plan(Long id, Career career, boolean current) {
        return StudyPlan.builder().id(id).identifier("2021.3").current(current).career(career).build();
    }

    private Student student(Long id, Career career, StudyPlan plan, StudentStatus status) {
        return Student.builder().id(id).run("1-" + id).fullName("Student " + id)
                .email("old" + id + "@ipt.cl").status(status).career(career).studyPlan(plan).build();
    }

    private void stubSaveReturnsArgument() {
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ findAll / findById

    @Test
    void findAllWithoutFiltersReturnsEveryStudent() {
        when(studentRepository.findAll()).thenReturn(List.of(
                student(1L, career1, plan1, StudentStatus.REGULAR),
                student(2L, career1, plan1, StudentStatus.POSTPONED),
                student(3L, career2, plan2, StudentStatus.REGULAR)));

        assertEquals(3, service.findAll(null, null).size());
    }

    @Test
    void findAllFiltersByStatus() {
        when(studentRepository.findAll()).thenReturn(List.of(
                student(1L, career1, plan1, StudentStatus.REGULAR),
                student(2L, career1, plan1, StudentStatus.POSTPONED),
                student(3L, career2, plan2, StudentStatus.REGULAR)));

        assertEquals(2, service.findAll(StudentStatus.REGULAR, null).size());
    }

    @Test
    void findAllFiltersByCareer() {
        when(studentRepository.findAll()).thenReturn(List.of(
                student(1L, career1, plan1, StudentStatus.REGULAR),
                student(3L, career2, plan2, StudentStatus.REGULAR)));

        List<StudentResponse> result = service.findAll(null, 2L);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).id());
    }

    @Test
    void findAllFiltersByStatusAndCareer() {
        when(studentRepository.findAll()).thenReturn(List.of(
                student(1L, career1, plan1, StudentStatus.REGULAR),
                student(2L, career1, plan1, StudentStatus.POSTPONED),
                student(3L, career2, plan2, StudentStatus.REGULAR)));

        List<StudentResponse> result = service.findAll(StudentStatus.REGULAR, 1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void findByIdReturnsStudent() {
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(student(1L, career1, plan1, StudentStatus.REGULAR)));

        StudentResponse response = service.findById(1L);

        assertEquals(1L, response.id());
        assertEquals(1L, response.careerId());
        assertEquals(5L, response.studyPlanId());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    // ------------------------------------------------------------------ create

    @Test
    void createNormalizesDataAndLinksCurrentPlan() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career1));
        when(studentRepository.existsByRun("12345678-9")).thenReturn(false);
        when(studentRepository.existsByEmail("ana@ipt.cl")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndCurrentTrue(1L)).thenReturn(Optional.of(plan1));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(9L);
            return s;
        });

        StudentResponse response = service.create(
                new StudentRequest(" 12345678-9 ", " Ana Perez ", " ANA@IPT.CL ", 1L));

        assertEquals(9L, response.id());
        assertEquals("12345678-9", response.run());
        assertEquals("Ana Perez", response.fullName());
        assertEquals("ana@ipt.cl", response.email());
        assertEquals(StudentStatus.REGULAR, response.status());
        assertEquals(5L, response.studyPlanId());
    }

    @Test
    void createThrowsWhenCareerDoesNotExist() {
        when(careerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(new StudentRequest("1-1", "Ana", "ana@ipt.cl", 7L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenRunAlreadyExists() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career1));
        when(studentRepository.existsByRun("1-1")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(new StudentRequest("1-1", "Ana", "ana@ipt.cl", 1L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenEmailAlreadyExists() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career1));
        when(studentRepository.existsByRun("1-1")).thenReturn(false);
        when(studentRepository.existsByEmail("ana@ipt.cl")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(new StudentRequest("1-1", "Ana", "ana@ipt.cl", 1L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveCareer() {
        career1.setStatus(CareerStatus.INACTIVE);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career1));
        when(studentRepository.existsByRun("1-1")).thenReturn(false);
        when(studentRepository.existsByEmail("ana@ipt.cl")).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.create(new StudentRequest("1-1", "Ana", "ana@ipt.cl", 1L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createRejectsCareerWithoutCurrentPlan() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career1));
        when(studentRepository.existsByRun("1-1")).thenReturn(false);
        when(studentRepository.existsByEmail("ana@ipt.cl")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndCurrentTrue(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.create(new StudentRequest("1-1", "Ana", "ana@ipt.cl", 1L)));
        verify(studentRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ update

    @Test
    void updateThrowsWhenStudentMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, new StudentUpdateRequest("Ana", "ana@ipt.cl", null, null)));
    }

    @Test
    void updateChangesNameAndEmailOnly() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(studentRepository.existsByEmail("new@ipt.cl")).thenReturn(false);
        stubSaveReturnsArgument();

        StudentResponse response = service.update(1L,
                new StudentUpdateRequest(" New Name ", " NEW@ipt.cl ", null, null));

        assertEquals("New Name", response.fullName());
        assertEquals("new@ipt.cl", response.email());
        assertEquals(1L, response.careerId());
        verifyNoInteractions(careerRepository, studyPlanRepository, enrollmentRepository, academicRecordRepository);
    }

    @Test
    void updateWithSameEmailInOtherCaseDoesNotCheckDuplicates() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        stubSaveReturnsArgument();

        StudentResponse response = service.update(1L,
                new StudentUpdateRequest("Same", "OLD1@IPT.CL", null, null));

        assertEquals("old1@ipt.cl", response.email());
        verify(studentRepository, never()).existsByEmail(any());
    }

    @Test
    void updateThrowsWhenEmailBelongsToAnotherStudent() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(studentRepository.existsByEmail("taken@ipt.cl")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.update(1L, new StudentUpdateRequest("Ana", "taken@ipt.cl", null, null)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateWithSameCareerAndPlanIsNotACareerChange() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        stubSaveReturnsArgument();

        service.update(1L, new StudentUpdateRequest("Ana", "old1@ipt.cl", 1L, 5L));

        verifyNoInteractions(enrollmentRepository, academicRecordRepository, careerRepository, studyPlanRepository);
    }

    @Test
    void updateRejectsCareerChangeWhenStudentHasEnrollments() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.update(1L, new StudentUpdateRequest("Ana", "old1@ipt.cl", 2L, null)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateRejectsCareerChangeWhenStudentHasAcademicHistory() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.update(1L, new StudentUpdateRequest("Ana", "old1@ipt.cl", 2L, null)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenNewCareerDoesNotExist() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);
        when(careerRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, new StudentUpdateRequest("Ana", "old1@ipt.cl", 2L, null)));
    }

    @Test
    void updateRejectsInactiveNewCareer() {
        career2.setStatus(CareerStatus.INACTIVE);
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);
        when(careerRepository.findById(2L)).thenReturn(Optional.of(career2));

        assertThrows(BusinessRuleException.class,
                () -> service.update(1L, new StudentUpdateRequest("Ana", "old1@ipt.cl", 2L, null)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateCareerChangeWithoutPlanUsesCurrentPlanOfNewCareer() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);
        when(careerRepository.findById(2L)).thenReturn(Optional.of(career2));
        when(studyPlanRepository.findByCareerIdAndCurrentTrue(2L)).thenReturn(Optional.of(plan2));
        stubSaveReturnsArgument();

        StudentResponse response = service.update(1L,
                new StudentUpdateRequest("Ana", "old1@ipt.cl", 2L, null));

        assertEquals(2L, response.careerId());
        assertEquals(6L, response.studyPlanId());
    }

    @Test
    void updateCareerChangeFailsWhenNewCareerHasNoCurrentPlan() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);
        when(careerRepository.findById(2L)).thenReturn(Optional.of(career2));
        when(studyPlanRepository.findByCareerIdAndCurrentTrue(2L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.update(1L, new StudentUpdateRequest("Ana", "old1@ipt.cl", 2L, null)));
    }

    @Test
    void updateCareerChangeWithExplicitPlanOfThatCareer() {
        StudyPlan otherPlan = plan(7L, career2, false);
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);
        when(careerRepository.findById(2L)).thenReturn(Optional.of(career2));
        when(studyPlanRepository.findById(7L)).thenReturn(Optional.of(otherPlan));
        stubSaveReturnsArgument();

        StudentResponse response = service.update(1L,
                new StudentUpdateRequest("Ana", "old1@ipt.cl", 2L, 7L));

        assertEquals(2L, response.careerId());
        assertEquals(7L, response.studyPlanId());
    }

    @Test
    void updateRejectsPlanOfAnotherCareer() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);
        when(careerRepository.findById(2L)).thenReturn(Optional.of(career2));
        when(studyPlanRepository.findById(5L)).thenReturn(Optional.of(plan1));

        // plan1 belongs to career1, but the student is moving to career2
        assertThrows(BusinessRuleException.class,
                () -> service.update(1L, new StudentUpdateRequest("Ana", "old1@ipt.cl", 2L, 5L)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenPlanDoesNotExist() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);
        when(studyPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, new StudentUpdateRequest("Ana", "old1@ipt.cl", null, 99L)));
    }

    @Test
    void updatePlanOnlyKeepsTheCareer() {
        StudyPlan newPlan = plan(8L, career1, false);
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);
        when(studyPlanRepository.findById(8L)).thenReturn(Optional.of(newPlan));
        stubSaveReturnsArgument();

        StudentResponse response = service.update(1L,
                new StudentUpdateRequest("Ana", "old1@ipt.cl", null, 8L));

        assertEquals(1L, response.careerId());
        assertEquals(8L, response.studyPlanId());
        verifyNoInteractions(careerRepository);
    }

    // ------------------------------------------------------------------ changeStatus

    @Test
    void changeStatusThrowsWhenStudentMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.changeStatus(99L, StudentStatus.POSTPONED));
    }

    @Test
    void changeStatusUpdatesStatus() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        stubSaveReturnsArgument();

        StudentResponse response = service.changeStatus(1L, StudentStatus.TEMPORARY_WITHDRAWAL);

        assertEquals(StudentStatus.TEMPORARY_WITHDRAWAL, response.status());
    }

    @Test
    void changeStatusCannotAssignGraduatedManually() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThrows(BusinessRuleException.class, () -> service.changeStatus(1L, StudentStatus.GRADUATED));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void changeStatusCannotAssignEliminatedManually() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThrows(BusinessRuleException.class, () -> service.changeStatus(1L, StudentStatus.ELIMINATED));
    }

    @Test
    void changeStatusCannotChangeGraduatedStudent() {
        Student s = student(1L, career1, plan1, StudentStatus.GRADUATED);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThrows(BusinessRuleException.class, () -> service.changeStatus(1L, StudentStatus.REGULAR));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void changeStatusCannotChangeEliminatedStudent() {
        Student s = student(1L, career1, plan1, StudentStatus.ELIMINATED);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThrows(BusinessRuleException.class, () -> service.changeStatus(1L, StudentStatus.REGULAR));
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deleteRemovesStudentWithoutEnrollmentsOrHistory() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(false);

        service.delete(1L);

        verify(studentRepository).delete(s);
    }

    @Test
    void deleteRejectsStudentWithEnrollments() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(studentRepository, never()).delete(any());
    }

    @Test
    void deleteRejectsStudentWithAcademicHistory() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(studentRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenStudentMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }

    // ------------------------------------------------------------------ academic history

    @Test
    void getAcademicHistoryReturnsRecords() {
        Student s = student(1L, career1, plan1, StudentStatus.REGULAR);
        AcademicPeriod period = AcademicPeriod.builder().id(1L).academicYear(2026).semester(1)
                .status(PeriodStatus.CLOSED).build();
        AcademicRecord record = AcademicRecord.builder().id(3L).student(s).period(period)
                .finalGrade(new BigDecimal("5.5")).result(CourseResult.PASSED)
                .courseCode("TAP101").courseName("Intro").build();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(academicRecordRepository.findByStudentId(1L)).thenReturn(List.of(record));

        List<AcademicRecordResponse> history = service.getAcademicHistory(1L);

        assertEquals(1, history.size());
        assertEquals("TAP101", history.get(0).courseCode());
        assertEquals("2026-1", history.get(0).period());
        assertEquals(CourseResult.PASSED, history.get(0).result());
    }

    @Test
    void getAcademicHistoryThrowsWhenStudentMissing() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getAcademicHistory(99L));
    }
}
