package com.example.campung.record.repository;

import com.example.campung.entity.Record;
import com.example.campung.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    
    List<Record> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT r FROM Record r WHERE r.recordId = :recordId AND r.user = :user")
    Optional<Record> findByRecordIdAndUser(@Param("recordId") Long recordId, @Param("user") User user);
}