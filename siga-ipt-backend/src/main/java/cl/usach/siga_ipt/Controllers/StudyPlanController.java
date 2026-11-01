package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.StudyPlanRequest;
import cl.usach.siga_ipt.Dtos.StudyPlanResponse;
import cl.usach.siga_ipt.Services.StudyPlanService;
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
@Tag(name = "Study plans", description = "Study plan management")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping("/careers/{careerId}/study-plans")
    @Operation(summary = "List the study plans of a career")
    public List<StudyPlanResponse> findByCareer(@PathVariable Long careerId) {
        return studyPlanService.findByCareer(careerId);
    }

    @PostMapping("/careers/{careerId}/study-plans")
    @Operation(summary = "Create a study plan for a career")
    public ResponseEntity<StudyPlanResponse> create(@PathVariable Long careerId,
                                                    @Valid @RequestBody StudyPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studyPlanService.create(careerId, request));
    }

    @GetMapping("/study-plans/{id}")
    @Operation(summary = "Get a study plan by id")
    public StudyPlanResponse findById(@PathVariable Long id) {
        return studyPlanService.findById(id);
    }

    @PatchMapping("/study-plans/{id}/current")
    @Operation(summary = "Make this the current plan of its career (the previous one becomes not current)")
    public StudyPlanResponse makeCurrent(@PathVariable Long id) {
        return studyPlanService.makeCurrent(id);
    }

    @DeleteMapping("/study-plans/{id}")
    @Operation(summary = "Delete a study plan without students or courses")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studyPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
