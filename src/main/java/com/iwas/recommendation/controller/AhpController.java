package com.iwas.recommendation.controller;

import com.iwas.recommendation.dto.AhpComparisonRequest;
import com.iwas.recommendation.dto.AhpWeightResponse;
import com.iwas.recommendation.service.AhpWeightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ahp")
@RequiredArgsConstructor
public class AhpController {

    private final AhpWeightService ahpWeightService;

    @PostMapping("/weights")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AhpWeightResponse submitWeights(@Valid @RequestBody AhpComparisonRequest request) {
        return ahpWeightService.submit(request);
    }

    @GetMapping("/weights/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public AhpWeightResponse getActive() {
        return ahpWeightService.getActive();
    }

    @GetMapping("/weights")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AhpWeightResponse> getAll() {
        return ahpWeightService.getAll();
    }

    @GetMapping("/weights/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AhpWeightResponse getById(@PathVariable Long id) {
        return ahpWeightService.getById(id);
    }

    @PutMapping("/weights/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public AhpWeightResponse activate(@PathVariable Long id) {
        return ahpWeightService.activate(id);
    }
}
