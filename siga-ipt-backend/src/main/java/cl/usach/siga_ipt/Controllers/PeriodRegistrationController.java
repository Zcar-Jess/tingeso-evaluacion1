package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.PeriodRegistrationRequest;
import cl.usach.siga_ipt.Dtos.PeriodRegistrationResponse;
import cl.usach.siga_ipt.Services.PeriodRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/periods/{periodId}/registrations")
@RequiredArgsConstructor
@Tag(name = "Period registrations", description = "Students registered (matriculados) in an academic period")
public class PeriodRegistrationController {

    private final PeriodRegistrationService registrationService;

    @PostMapping
    @Operation(summary = "Record that a student is registered in the period")
    public ResponseEntity<PeriodRegistrationResponse> register(@PathVariable Long periodId,
                                                               @Valid @RequestBody PeriodRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(periodId, request.studentId()));
    }

    @GetMapping
    @Operation(summary = "List the students registered in the period")
    public List<PeriodRegistrationResponse> findByPeriod(@PathVariable Long periodId) {
        return registrationService.findByPeriod(periodId);
    }
}
