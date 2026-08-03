package com.manekpay.auth.auth;

import com.manekpay.auth.customer.Customer;
import com.manekpay.auth.customer.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
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
}
