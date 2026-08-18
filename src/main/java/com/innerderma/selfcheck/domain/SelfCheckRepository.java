package com.innerderma.selfcheck.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SelfCheckRepository extends JpaRepository<SelfCheck, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<SelfCheck> findFirstByUser_UserCodeOrderByCheckedAtDesc(String userCode);

    @EntityGraph(attributePaths = "user")
    List<SelfCheck> findByUser_UserCodeAndCheckedAtBetweenOrderByCheckedAtDesc(
            String userCode, LocalDateTime from, LocalDateTime to);
}
