package com.booking.service;

import com.booking.dto.request.ResourceRequest;
import com.booking.dto.response.ResourceResponse;
import com.booking.entity.Resource;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAllResources(Pageable pageable) {
        return resourceRepository.findAll(pageable)
                .map(ResourceResponse::from);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = findResourceOrThrow(id);
        return ResourceResponse.from(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .capacity(request.getCapacity())
                .pricePerHour(request.getPricePerHour())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .build();

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findResourceOrThrow(id);

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setCapacity(request.getCapacity());
        resource.setPricePerHour(request.getPricePerHour());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = findResourceOrThrow(id);
        resourceRepository.delete(resource);
    }

    private Resource findResourceOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
    }
}
