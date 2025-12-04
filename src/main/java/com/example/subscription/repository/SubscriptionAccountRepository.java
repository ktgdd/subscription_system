package com.example.subscription.repository;

import com.example.subscription.model.SubscriptionAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionAccountRepository extends JpaRepository<SubscriptionAccount, Long> {
    
    Optional<SubscriptionAccount> findByName(String name);
    
    boolean existsByName(String name);
}

