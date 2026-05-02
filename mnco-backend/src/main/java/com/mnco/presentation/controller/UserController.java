package com.mnco.presentation.controller;

import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.UserResponse;
import com.mnco.application.mapper.UserMapper;
import com.mnco.domain.entities.UserRole;
import com.mnco.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestParam(required = false) UserRole role) {
        
        List<UserResponse> users = userRepository.findAll().stream()
                .filter(u -> role == null || u.getRole().equals(role))
                .map(userMapper::toResponse)
                .toList();
                
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}
