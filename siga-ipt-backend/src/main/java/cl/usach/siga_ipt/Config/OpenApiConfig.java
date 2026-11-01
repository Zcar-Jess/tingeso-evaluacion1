package cl.usach.siga_ipt.Config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API in Swagger UI (/swagger-ui.html) and adds the "Authorize" button, where the
 * Keycloak access token is pasted to try the protected endpoints.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SIGA IPT API",
                version = "1.0",
                description = "Sistema Integrado de Gestión Académica del Instituto Profesional de Tecnología"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
