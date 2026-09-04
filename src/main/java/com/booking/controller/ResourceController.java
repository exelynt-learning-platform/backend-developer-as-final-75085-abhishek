package com.booking.controller;

import com.booking.dto.request.ResourceRequest;
import com.booking.dto.response.ResourceResponse;
import com.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Manage bookable resources")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(
        summary = "List all resources",
        description = "Accessible to all authenticated users. Supports pagination and sorting."
    )
    public ResponseEntity<Page<ResourceResponse>> getAllResources(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(resourceService.getAllResources(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID", description = "Accessible to all authenticated users.")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create resource", description = "ADMIN only.")
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.createResource(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update resource", description = "ADMIN only.")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.updateResource(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete resource", description = "ADMIN only.")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
