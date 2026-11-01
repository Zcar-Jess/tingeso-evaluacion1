package cl.usach.siga_ipt.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Production security: every request needs a JWT issued by Keycloak. Roles come from the
 * {@code realm_access.roles} claim (ADMIN, TEACHER, STUDENT) and become ROLE_ADMIN, etc.
 *
 * <p>This class sets the access rules by role for each endpoint. The extra "own data only"
 * rules (a student sees only himself, a teacher grades only his sections) are checked in the
 * controllers with {@code @PreAuthorize} and {@code CurrentUserService}.
 *
 * <p>It is disabled with the {@code dev} profile, where {@link DevSecurityConfig} opens everything.
 */
@Configuration
@EnableMethodSecurity
@Profile("!dev")
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // API documentation
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // health check used by Docker (is the instance ready?)
                        .requestMatchers("/actuator/health/**").permitAll()

                        // identity of the logged-in user
                        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()

                        // students: enrollment and own data
                        .requestMatchers(HttpMethod.POST, "/api/periods/*/enrollments").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/enrollments/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET,
                                "/api/students/*",
                                "/api/students/*/academic-history",
                                "/api/students/*/enrollments",
                                "/api/students/*/available-sections").hasAnyRole("ADMIN", "STUDENT")
                        .requestMatchers("/api/students/**").hasRole("ADMIN")

                        // teachers: own sections and grades
                        .requestMatchers(HttpMethod.PUT, "/api/sections/*/students/*/grade").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET,
                                "/api/sections/*/enrollments",
                                "/api/teachers/*/sections").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/teachers/**").hasRole("ADMIN")

                        // catalog: readable by any authenticated user
                        .requestMatchers(HttpMethod.GET,
                                "/api/careers/**",
                                "/api/study-plans/**",
                                "/api/courses/**",
                                "/api/periods",
                                "/api/periods/*",
                                "/api/periods/*/sections",
                                "/api/sections/*").authenticated()

                        // everything else in the API (create/update/delete, registrations, closing)
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Maps the Keycloak realm roles to Spring authorities with the ROLE_ prefix. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
            return authorities;
        });
        return converter;
    }
}