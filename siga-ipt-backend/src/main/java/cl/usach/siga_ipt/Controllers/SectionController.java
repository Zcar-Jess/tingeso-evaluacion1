package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.SectionRequest;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Dtos.SectionUpdateRequest;
import cl.usach.siga_ipt.Services.SectionService;
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
@Tag(name = "Sections", description = "Academic offer: sections of an academic period")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/periods/{periodId}/sections")
    @Operation(summary = "List the sections (academic offer) of a period")
    public List<SectionResponse> findByPeriod(@PathVariable Long periodId) {
        return sectionService.findByPeriod(periodId);
    }

    @PostMapping("/periods/{periodId}/sections")
    @Operation(summary = "Create the section of a course in a period")
    public ResponseEntity<SectionResponse> create(@PathVariable Long periodId,
                                                  @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.create(periodId, request));
    }

    @GetMapping("/sections/{id}")
    @Operation(summary = "Get a section by id")
    public SectionResponse findById(@PathVariable Long id) {
        return sectionService.findById(id);
    }

    @PutMapping("/sections/{id}")
    @Operation(summary = "Update teacher, capacity and schedule of a section")
    public SectionResponse update(@PathVariable Long id, @Valid @RequestBody SectionUpdateRequest request) {
        return sectionService.update(id, request);
    }

    @DeleteMapping("/sections/{id}")
    @Operation(summary = "Delete a section without enrolled students")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
