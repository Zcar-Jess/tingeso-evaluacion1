package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.CourseRequest;
import cl.usach.siga_ipt.Dtos.CourseResponse;
import cl.usach.siga_ipt.Services.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course and prerequisite management")
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/study-plans/{studyPlanId}/courses")
    @Operation(summary = "List the courses of a study plan, ordered by semester")
    public List<CourseResponse> findByStudyPlan(@PathVariable Long studyPlanId) {
        return courseService.findByStudyPlan(studyPlanId);
    }

    @PostMapping("/study-plans/{studyPlanId}/courses")
    @Operation(summary = "Create a course in a study plan")
    public ResponseEntity<CourseResponse> create(@PathVariable Long studyPlanId,
                                                 @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(studyPlanId, request));
    }

    @GetMapping("/courses/{id}")
    @Operation(summary = "Get a course by id")
    public CourseResponse findById(@PathVariable Long id) {
        return courseService.findById(id);
    }

    @PutMapping("/courses/{id}")
    @Operation(summary = "Update a course and its prerequisites")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/courses/{id}")
    @Operation(summary = "Delete a course without history, sections or dependants")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
