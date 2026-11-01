package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.AcademicPeriodRequest;
import cl.usach.siga_ipt.Dtos.AcademicPeriodResponse;
import cl.usach.siga_ipt.Services.AcademicPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/periods")
@RequiredArgsConstructor
@Tag(name = "Academic periods", description = "Academic period management")
public class AcademicPeriodController {

    private final AcademicPeriodService periodService;

    @GetMapping
    @Operation(summary = "List academic periods, newest first")
    public List<AcademicPeriodResponse> findAll() {
        return periodService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an academic period by id")
    public AcademicPeriodResponse findById(@PathVariable Long id) {
        return periodService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create an academic period (starts OPEN)")
    public ResponseEntity<AcademicPeriodResponse> create(@Valid @RequestBody AcademicPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodService.create(request));
    }
}
