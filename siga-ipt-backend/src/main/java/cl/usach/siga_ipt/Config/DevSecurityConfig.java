package cl.usach.siga_ipt.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Only active with the {@code dev} profile (SPRING_PROFILES_ACTIVE=dev): every endpoint is open,
 * with no Keycloak. Useful to run the test scripts and to work without the identity server.
 * Never use it in production: the default profile uses {@link SecurityConfig}.
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}