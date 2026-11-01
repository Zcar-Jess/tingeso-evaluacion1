package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.PeriodClosureResponse;
import cl.usach.siga_ipt.Services.PeriodClosureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/periods")
@RequiredArgsConstructor
@Tag(name = "Period closing", description = "Closing of an academic period")
public class PeriodClosureController {

    private final PeriodClosureService closureService;

    @PostMapping("/{periodId}/close")
    @Operation(summary = "Close the period: history, ELIMINATED / GRADUATED students, status CLOSED")
    public PeriodClosureResponse close(@PathVariable Long periodId) {
        return closureService.close(periodId);
    }
}
