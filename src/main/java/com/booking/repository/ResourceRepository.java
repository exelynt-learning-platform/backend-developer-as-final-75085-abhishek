package com.booking.repository;

import com.booking.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Page<Resource> findByAvailable(Boolean available, Pageable pageable);
}
