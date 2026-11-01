package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.EnrollmentRequest;
import cl.usach.siga_ipt.Dtos.EnrollmentResponse;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Services.EnrollmentService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Academic enrollment of students in a period")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/periods/{periodId}/enrollments")
    @PreAuthorize("@currentUser.isStudent(#request.studentId())")
    @Operation(summary = "Enroll the selected courses (the logged-in student only; all or nothing)")
    public ResponseEntity<List<EnrollmentResponse>> enroll(@PathVariable Long periodId,
                                                           @Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(periodId, request));
    }

    @DeleteMapping("/enrollments/{id}")
    @PreAuthorize("@currentUser.ownsEnrollment(#id)")
    @Operation(summary = "Cancel an own enrollment (period OPEN, no grade registered)")
    public ResponseEntity<Void> annul(@PathVariable Long id) {
        enrollmentService.annul(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/students/{studentId}/enrollments")
    @PreAuthorize("hasRole('ADMIN') or @currentUser.isStudent(#studentId)")
    @Operation(summary = "Enrollments of a student in a period (administrator, or the student himself)")
    public List<EnrollmentResponse> findByStudent(@PathVariable Long studentId, @RequestParam Long periodId) {
        return enrollmentService.findByStudent(studentId, periodId);
    }

    @GetMapping("/students/{studentId}/available-sections")
    @PreAuthorize("hasRole('ADMIN') or @currentUser.isStudent(#studentId)")
    @Operation(summary = "Sections the student can enroll in during the period")
    public List<SectionResponse> findAvailableSections(@PathVariable Long studentId, @RequestParam Long periodId) {
        return enrollmentService.findAvailableSections(studentId, periodId);
    }

    @GetMapping("/sections/{sectionId}/enrollments")
    @PreAuthorize("hasRole('ADMIN') or @currentUser.teachesSection(#sectionId)")
    @Operation(summary = "Students enrolled in a section (administrator, or the section's teacher)")
    public List<EnrollmentResponse> findBySection(@PathVariable Long sectionId) {
        return enrollmentService.findBySection(sectionId);
    }
}