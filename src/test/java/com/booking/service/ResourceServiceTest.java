package com.booking.service;

import com.booking.dto.request.ResourceRequest;
import com.booking.dto.response.ResourceResponse;
import com.booking.entity.Resource;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource sampleResource;
    private ResourceRequest resourceRequest;

    @BeforeEach
    void setUp() {
        sampleResource = Resource.builder()
                .id(1L)
                .name("Conference Room A")
                .description("A spacious room")
                .type("ROOM")
                .capacity(20)
                .pricePerHour(new BigDecimal("50.00"))
                .available(true)
                .build();

        resourceRequest = new ResourceRequest();
        resourceRequest.setName("Conference Room A");
        resourceRequest.setDescription("A spacious room");
        resourceRequest.setType("ROOM");
        resourceRequest.setCapacity(20);
        resourceRequest.setPricePerHour(new BigDecimal("50.00"));
        resourceRequest.setAvailable(true);
    }

    @Test
    void getAllResources_shouldReturnPagedResults() {
        Page<Resource> page = new PageImpl<>(List.of(sampleResource));
        when(resourceRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<ResourceResponse> result = resourceService.getAllResources(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Conference Room A");
        verify(resourceRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getResourceById_existingId_shouldReturnResource() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));

        ResourceResponse result = resourceService.getResourceById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Conference Room A");
        assertThat(result.getPricePerHour()).isEqualByComparingTo("50.00");
    }

    @Test
    void getResourceById_nonExistingId_shouldThrowNotFoundException() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getResourceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createResource_shouldSaveAndReturnResource() {
        when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

        ResourceResponse result = resourceService.createResource(resourceRequest);

        assertThat(result.getName()).isEqualTo("Conference Room A");
        assertThat(result.getAvailable()).isTrue();
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void updateResource_existingId_shouldUpdateAndReturn() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(sampleResource);

        resourceRequest.setName("Updated Room");
        ResourceResponse result = resourceService.updateResource(1L, resourceRequest);

        verify(resourceRepository).save(any(Resource.class));
        assertThat(result).isNotNull();
    }

    @Test
    void updateResource_nonExistingId_shouldThrowNotFoundException() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.updateResource(99L, resourceRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteResource_existingId_shouldDelete() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(sampleResource));

        resourceService.deleteResource(1L);

        verify(resourceRepository).delete(sampleResource);
    }

    @Test
    void deleteResource_nonExistingId_shouldThrowNotFoundException() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.deleteResource(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
