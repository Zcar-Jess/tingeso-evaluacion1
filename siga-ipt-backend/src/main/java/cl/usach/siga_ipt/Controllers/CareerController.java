package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.CareerRequest;
import cl.usach.siga_ipt.Dtos.CareerResponse;
import cl.usach.siga_ipt.Dtos.CareerStatusRequest;
import cl.usach.siga_ipt.Services.CareerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
@Tag(name = "Careers", description = "Career management")
public class CareerController {

    private final CareerService careerService;

    @GetMapping
    @Operation(summary = "List all careers")
    public List<CareerResponse> findAll() {
        return careerService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a career by id")
    public CareerResponse findById(@PathVariable Long id) {
        return careerService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create a career (starts ACTIVE)")
    public ResponseEntity<CareerResponse> create(@Valid @RequestBody CareerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(careerService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update career data")
    public CareerResponse update(@PathVariable Long id, @Valid @RequestBody CareerRequest request) {
        return careerService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change career status (ACTIVE / INACTIVE)")
    public CareerResponse changeStatus(@PathVariable Long id, @Valid @RequestBody CareerStatusRequest request) {
        return careerService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a career without students or study plans")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        careerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
