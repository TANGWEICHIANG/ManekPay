package com.manekpay.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // ponytail: stateless JSON API (no browser sessions), so CSRF protection doesn't apply here.
    // /register is public; later auth tasks (login, etc.) add their own permitted paths and a JWT filter.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/actuator/health").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
