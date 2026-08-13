package com.manekpay.vaults.controller;

import com.manekpay.vaults.dto.CreateGoalRequest;
import com.manekpay.vaults.dto.GoalResponse;
import com.manekpay.vaults.dto.UpdateGoalRequest;
import com.manekpay.vaults.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateGoalRequest request) {
        return goalService.create(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping
    public List<GoalResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return goalService.list(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/{id}")
    public GoalResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody UpdateGoalRequest request) {
        return goalService.update(UUID.fromString(jwt.getSubject()), id, request);
    }
}
