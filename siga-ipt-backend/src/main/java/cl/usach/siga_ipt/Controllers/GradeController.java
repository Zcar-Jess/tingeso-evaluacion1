package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.EnrollmentResponse;
import cl.usach.siga_ipt.Dtos.GradeRequest;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Services.CurrentUserService;
import cl.usach.siga_ipt.Services.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Grades", description = "Final grades registered by the responsible teacher")
public class GradeController {

    private final GradeService gradeService;
    private final CurrentUserService currentUserService;

    @PutMapping("/sections/{sectionId}/students/{studentId}/grade")
    @Operation(summary = "Register or modify a final grade (the teacher of the section, period OPEN)")
    public EnrollmentResponse registerGrade(@PathVariable Long sectionId,
                                            @PathVariable Long studentId,
                                            @Valid @RequestBody GradeRequest request) {
        // With a token the teacher is the one linked to it; the body's teacherId is ignored.
        Long teacherId = currentUserService.resolveTeacherId(request.teacherId());
        return gradeService.registerGrade(sectionId, studentId, new GradeRequest(teacherId, request.finalGrade()));
    }

    @GetMapping("/teachers/{teacherId}/sections")
    @PreAuthorize("hasRole('ADMIN') or @currentUser.isTeacher(#teacherId)")
    @Operation(summary = "Sections assigned to a teacher (administrator, or the teacher himself)")
    public List<SectionResponse> findSectionsByTeacher(@PathVariable Long teacherId,
                                                       @RequestParam(required = false) Long periodId) {
        return gradeService.findSectionsByTeacher(teacherId, periodId);
    }
}