package cl.usach.siga_ipt.Controllers;

import cl.usach.siga_ipt.Dtos.MeResponse;
import cl.usach.siga_ipt.Services.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Identity", description = "Logged-in user")
public class MeController {

    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Role and linked student/teacher of the user in the access token")
    public MeResponse me() {
        return currentUserService.me();
    }
}
