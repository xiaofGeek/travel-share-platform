package com.travelshare.platform.controller;

import com.travelshare.platform.common.ApiResponse;
import com.travelshare.platform.dto.AuditRequest;
import com.travelshare.platform.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService service;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    @GetMapping("/guides")
    public ApiResponse<?> guides(Authentication authentication,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "20") long size) {
        String requestedStatus = status == null ? null : status.trim().toUpperCase();
        String effectiveStatus = hasRole(authentication, "AUDITOR")
                ? ("APPROVED".equals(requestedStatus) ? "APPROVED" : "PENDING")
                : requestedStatus;
        return ApiResponse.ok(service.guides(effectiveStatus, keyword, page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    @PostMapping("/guides/{id}/audit")
    public ApiResponse<Void> audit(Authentication authentication, @PathVariable Long id,
                                   @Valid @RequestBody AuditRequest request) {
        service.auditGuide(authentication.getName(), id, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    @PostMapping("/guides/{id}/revoke")
    public ApiResponse<Void> revokeApproval(Authentication authentication, @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        service.revokeGuideApproval(authentication.getName(), id, body.get("reason"));
        return ApiResponse.ok();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ApiResponse<?> users(@RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(service.users(keyword, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/destinations")
    public ApiResponse<?> destinations(@RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(service.destinations(keyword, page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    @GetMapping("/reports")
    public ApiResponse<?> reports() {
        return ApiResponse.ok(service.reports());
    }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    @PostMapping("/reports/{id}/handle")
    public ApiResponse<Void> handleReport(Authentication authentication, @PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        service.handleReport(authentication.getName(), id, body.get("result"), body.get("note"));
        return ApiResponse.ok();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/routes")
    public ApiResponse<?> routes(@RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(service.routes(keyword, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/topics")
    public ApiResponse<?> topics(@RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(service.topics(keyword, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/comments")
    public ApiResponse<?> comments(@RequestParam(required = false) String keyword,
                                   @RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(service.comments(keyword, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(Authentication authentication, @PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        service.deleteComment(authentication.getName(), id, body == null ? null : body.get("reason"));
        return ApiResponse.ok();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/banners")
    public ApiResponse<?> banners(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.banners(keyword));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/recommendations")
    public ApiResponse<?> recommendations(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.recommendations(keyword));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/configs")
    public ApiResponse<?> configs(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.configs(keyword));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/logs")
    public ApiResponse<?> logs(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.logs(keyword));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{resource}/{id}")
    public ApiResponse<?> detail(@PathVariable String resource, @PathVariable Long id) {
        return ApiResponse.ok(service.detail(resource, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{resource}")
    public ApiResponse<?> create(Authentication authentication, @PathVariable String resource,
                                 @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.save(authentication.getName(), resource, null, body));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{resource}/{id}")
    public ApiResponse<?> update(Authentication authentication, @PathVariable String resource,
                                 @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.save(authentication.getName(), resource, id, body));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/status")
    public ApiResponse<Void> toggleUser(Authentication authentication, @PathVariable Long id) {
        service.toggleUser(authentication.getName(), id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/destinations/{id}/recommend")
    public ApiResponse<Void> toggleDestination(@PathVariable Long id) {
        service.toggleDestination(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/banners/{id}/status")
    public ApiResponse<Void> toggleBanner(@PathVariable Long id) {
        service.toggleBanner(id);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/guides/{id}/offline")
    public ApiResponse<Void> offlineGuide(Authentication authentication, @PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        service.offlineGuide(authentication.getName(), id, body.get("reason"));
        return ApiResponse.ok();
    }

    private static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) return false;
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream().anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
