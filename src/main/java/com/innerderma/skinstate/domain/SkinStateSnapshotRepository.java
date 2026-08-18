package com.innerderma.skinstate.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SkinStateSnapshotRepository extends JpaRepository<SkinStateSnapshot, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<SkinStateSnapshot> findByUser_UserCodeAndSnapshotDate(String userCode, LocalDate snapshotDate);

    @EntityGraph(attributePaths = "user")
    Optional<SkinStateSnapshot> findFirstByUser_UserCodeOrderBySnapshotDateDesc(String userCode);
}
