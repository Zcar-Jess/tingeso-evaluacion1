package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.AcademicRecordResponse;
import cl.usach.siga_ipt.Dtos.StudentRequest;
import cl.usach.siga_ipt.Dtos.StudentResponse;
import cl.usach.siga_ipt.Dtos.StudentStatusRequest;
import cl.usach.siga_ipt.Dtos.StudentUpdateRequest;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Services.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student management")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "List students, optionally filtered by status and/or career")
    public List<StudentResponse> findAll(@RequestParam(required = false) StudentStatus status,
                                         @RequestParam(required = false) Long careerId) {
        return studentService.findAll(status, careerId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @currentUser.isStudent(#id)")
    @Operation(summary = "Get a student by id (administrator, or the student himself)")
    public StudentResponse findById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Register a student (REGULAR, linked to the career's current study plan)")
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update student data (career/plan only without enrollments or history)")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Set REGULAR, POSTPONED or TEMPORARY_WITHDRAWAL")
    public StudentResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StudentStatusRequest request) {
        return studentService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a student without enrollments or academic history")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/academic-history")
    @PreAuthorize("hasRole('ADMIN') or @currentUser.isStudent(#id)")
    @Operation(summary = "Academic history of a student (administrator, or the student himself)")
    public List<AcademicRecordResponse> academicHistory(@PathVariable Long id) {
        return studentService.getAcademicHistory(id);
    }
}