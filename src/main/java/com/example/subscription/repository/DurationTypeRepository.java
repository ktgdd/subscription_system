package com.example.subscription.repository;

import com.example.subscription.model.DurationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DurationTypeRepository extends JpaRepository<DurationType, Long> {
    
    Optional<DurationType> findByType(String type);
}

