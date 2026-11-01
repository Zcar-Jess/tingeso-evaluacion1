package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.TeacherRequest;
import cl.usach.siga_ipt.Dtos.TeacherResponse;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Entities.TeacherStatus;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock private TeacherRepository teacherRepository;
    @Mock private SectionRepository sectionRepository;
    @InjectMocks private TeacherService service;

    private Teacher teacher(Long id, TeacherStatus status) {
        return Teacher.builder().id(id).run("9000000" + id + "-1").fullName("Teacher " + id)
                .email("teacher" + id + "@ipt.cl").professionalTitle("Engineer")
                .academicDegree("Master").status(status).build();
    }

    private TeacherRequest request(String run, String email) {
        return new TeacherRequest(run, " New Name ", email, " Engineer ", " PhD ");
    }

    private void stubSaveReturnsArgument() {
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ queries

    @Test
    void findAllWithoutFilterReturnsEveryTeacher() {
        when(teacherRepository.findAll()).thenReturn(List.of(
                teacher(1L, TeacherStatus.ACTIVE), teacher(2L, TeacherStatus.INACTIVE)));

        assertEquals(2, service.findAll(null).size());
    }

    @Test
    void findAllFiltersByStatus() {
        when(teacherRepository.findAll()).thenReturn(List.of(
                teacher(1L, TeacherStatus.ACTIVE), teacher(2L, TeacherStatus.INACTIVE)));

        List<TeacherResponse> result = service.findAll(TeacherStatus.INACTIVE);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
    }

    @Test
    void findByIdReturnsTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, TeacherStatus.ACTIVE)));

        assertEquals("Teacher 1", service.findById(1L).fullName());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    // ------------------------------------------------------------------ create

    @Test
    void createNormalizesDataAndStartsActive() {
        when(teacherRepository.existsByRun("11111111-1")).thenReturn(false);
        when(teacherRepository.existsByEmail("new@ipt.cl")).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> {
            Teacher t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });

        TeacherResponse response = service.create(request(" 11111111-1 ", " NEW@ipt.cl "));

        assertEquals(5L, response.id());
        assertEquals("11111111-1", response.run());
        assertEquals("New Name", response.fullName());
        assertEquals("new@ipt.cl", response.email());
        assertEquals("Engineer", response.professionalTitle());
        assertEquals("PhD", response.academicDegree());
        assertEquals(TeacherStatus.ACTIVE, response.status());
    }

    @Test
    void createRejectsDuplicatedRun() {
        when(teacherRepository.existsByRun("11111111-1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request("11111111-1", "new@ipt.cl")));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicatedEmail() {
        when(teacherRepository.existsByRun("11111111-1")).thenReturn(false);
        when(teacherRepository.existsByEmail("new@ipt.cl")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request("11111111-1", "new@ipt.cl")));
        verify(teacherRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ update

    @Test
    void updateThrowsWhenMissing() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, request("1-1", "a@ipt.cl")));
    }

    @Test
    void updateWithUnchangedRunAndEmailDoesNotCheckDuplicates() {
        Teacher t = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        stubSaveReturnsArgument();

        TeacherResponse response = service.update(1L, request(t.getRun(), t.getEmail().toUpperCase()));

        assertEquals("New Name", response.fullName());
        assertEquals("teacher1@ipt.cl", response.email());
        verify(teacherRepository, never()).existsByRun(any());
        verify(teacherRepository, never()).existsByEmail(any());
    }

    @Test
    void updateChangesRunAndEmailWhenFree() {
        Teacher t = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        when(teacherRepository.existsByRun("22222222-2")).thenReturn(false);
        when(teacherRepository.existsByEmail("other@ipt.cl")).thenReturn(false);
        stubSaveReturnsArgument();

        TeacherResponse response = service.update(1L, request("22222222-2", "other@ipt.cl"));

        assertEquals("22222222-2", response.run());
        assertEquals("other@ipt.cl", response.email());
        assertEquals("PhD", response.academicDegree());
    }

    @Test
    void updateRejectsRunUsedByAnotherTeacher() {
        Teacher t = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        when(teacherRepository.existsByRun("22222222-2")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(1L, request("22222222-2", t.getEmail())));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void updateRejectsEmailUsedByAnotherTeacher() {
        Teacher t = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        when(teacherRepository.existsByEmail("other@ipt.cl")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(1L, request(t.getRun(), "other@ipt.cl")));
        verify(teacherRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ changeStatus

    @Test
    void changeStatusThrowsWhenMissing() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.changeStatus(99L, TeacherStatus.INACTIVE));
    }

    @Test
    void changeStatusToInactiveRejectedWhenTeacherHasSectionsInOpenPeriod() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, TeacherStatus.ACTIVE)));
        when(sectionRepository.existsByTeacherIdAndPeriodStatus(1L, PeriodStatus.OPEN)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.changeStatus(1L, TeacherStatus.INACTIVE));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void changeStatusToInactiveAllowedWithoutOpenSections() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, TeacherStatus.ACTIVE)));
        when(sectionRepository.existsByTeacherIdAndPeriodStatus(1L, PeriodStatus.OPEN)).thenReturn(false);
        stubSaveReturnsArgument();

        assertEquals(TeacherStatus.INACTIVE, service.changeStatus(1L, TeacherStatus.INACTIVE).status());
    }

    @Test
    void changeStatusToActiveDoesNotCheckSections() {
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher(2L, TeacherStatus.INACTIVE)));
        stubSaveReturnsArgument();

        assertEquals(TeacherStatus.ACTIVE, service.changeStatus(2L, TeacherStatus.ACTIVE).status());
        verify(sectionRepository, never()).existsByTeacherIdAndPeriodStatus(any(), any());
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deleteRemovesTeacherWithoutSections() {
        Teacher t = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        when(sectionRepository.existsByTeacherId(1L)).thenReturn(false);

        service.delete(1L);

        verify(teacherRepository).delete(t);
    }

    @Test
    void deleteRejectsTeacherWithSections() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, TeacherStatus.ACTIVE)));
        when(sectionRepository.existsByTeacherId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(teacherRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }
}
