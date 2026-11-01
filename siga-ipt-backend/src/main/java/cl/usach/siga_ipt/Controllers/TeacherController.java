package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.TeacherRequest;
import cl.usach.siga_ipt.Dtos.TeacherResponse;
import cl.usach.siga_ipt.Dtos.TeacherStatusRequest;
import cl.usach.siga_ipt.Entities.TeacherStatus;
import cl.usach.siga_ipt.Services.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Teachers", description = "Teacher management")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @Operation(summary = "List teachers, optionally filtered by status")
    public List<TeacherResponse> findAll(@RequestParam(required = false) TeacherStatus status) {
        return teacherService.findAll(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a teacher by id")
    public TeacherResponse findById(@PathVariable Long id) {
        return teacherService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Register a teacher (starts ACTIVE)")
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update teacher data")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return teacherService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change teacher status (not INACTIVE with sections in an OPEN period)")
    public TeacherResponse changeStatus(@PathVariable Long id, @Valid @RequestBody TeacherStatusRequest request) {
        return teacherService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a teacher without sections")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
