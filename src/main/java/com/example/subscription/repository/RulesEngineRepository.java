package com.example.subscription.repository;

import com.example.subscription.model.RulesEngine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RulesEngineRepository extends JpaRepository<RulesEngine, Long> {
    
    Optional<RulesEngine> findByRuleKeyAndIsActiveTrue(String ruleKey);
}

