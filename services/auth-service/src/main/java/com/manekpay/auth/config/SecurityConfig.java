package com.manekpay.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manekpay.auth.auth.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // ponytail: Spring Security auto-registers a LogoutFilter on POST /logout and a
                // 403-by-default entry point for anonymous access; both fight our own /logout
                // controller and the 401-on-no-token contract, so turn off the former and pin the latter.
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    // ponytail: sendError() would forward to BasicErrorController, whose default
                    // DefaultErrorAttributes omits `message` — write the same ErrorResponse shape
                    // ApiExceptionHandler uses so every 401 has a consistent body. Reuse the app's
                    // Spring-managed ObjectMapper bean (not a fresh `new ObjectMapper()`) so the
                    // `timestamp` field renders as the same ISO-8601 string Boot's Jackson
                    // auto-config produces everywhere else, instead of a raw epoch number.
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    ErrorResponse body = new ErrorResponse(Instant.now(), HttpServletResponse.SC_UNAUTHORIZED,
                            "Unauthorized", "Full authentication is required to access this resource",
                            request.getRequestURI());
                    objectMapper.writeValue(response.getWriter(), body);
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/login", "/refresh", "/logout", "/.well-known/jwks.json", "/actuator/health", "/error")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
