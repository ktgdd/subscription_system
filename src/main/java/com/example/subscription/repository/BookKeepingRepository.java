package com.example.subscription.repository;

import com.example.subscription.model.BookKeeping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookKeepingRepository extends JpaRepository<BookKeeping, Long> {
    
    Optional<BookKeeping> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT bk FROM BookKeeping bk WHERE bk.status = 'COMPLETED' AND bk.processedAt IS NULL ORDER BY bk.completedAt ASC")
    List<BookKeeping> findPendingCompletedEntries();
}

