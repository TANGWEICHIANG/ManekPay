package com.manekpay.auth.controller;
import com.manekpay.auth.dto.MeResponse;
import com.manekpay.auth.dto.RegisterResponse;
import com.manekpay.auth.exception.InvalidCredentialsException;
import com.manekpay.auth.exception.DuplicateEmailException;
import com.manekpay.auth.service.RefreshTokenService;
import com.manekpay.auth.dto.LoginRequest;
import com.manekpay.auth.dto.RegisterRequest;
import com.manekpay.auth.exception.InvalidTokenException;
import com.manekpay.auth.dto.TokenResponse;
import com.manekpay.auth.dto.RefreshRequest;
import com.manekpay.auth.service.JwtService;

import com.manekpay.auth.entity.Customer;
import com.manekpay.auth.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AuthController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(CustomerRepository customerRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        if (customerRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }
        Customer customer = new Customer(request.email(), passwordEncoder.encode(request.password()), request.fullName());
        Customer saved = customerRepository.save(customer);
        return new RegisterResponse(saved.getId(), saved.getKycStatus());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String accessToken = jwtService.issueAccessToken(customer.getId(), customer.getEmail(), customer.getKycStatus());
        String refreshToken = refreshTokenService.issue(customer.getId());
        return new TokenResponse(accessToken, refreshToken, JwtService.ACCESS_TOKEN_TTL.toSeconds());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        UUID customerId = refreshTokenService.consume(request.refreshToken())
                .orElseThrow(InvalidTokenException::new);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(InvalidTokenException::new);
        String accessToken = jwtService.issueAccessToken(customer.getId(), customer.getEmail(), customer.getKycStatus());
        String newRefreshToken = refreshTokenService.issue(customer.getId());
        return new TokenResponse(accessToken, newRefreshToken, JwtService.ACCESS_TOKEN_TTL.toSeconds());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.consume(request.refreshToken());
    }

    @GetMapping("/me")
    public MeResponse me() {
        UUID customerId = UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(InvalidTokenException::new);
        return new MeResponse(customer.getId(), customer.getEmail(), customer.getFullName(), customer.getKycStatus());
    }
}
