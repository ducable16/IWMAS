package com.iwas.recommendation.repository;

import com.iwas.recommendation.entity.AhpWeightSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AhpWeightSetRepository extends JpaRepository<AhpWeightSet, Long> {

    /** Returns the currently applied weight set, if any. */
    @Query("SELECT a FROM AhpWeightSet a WHERE a.isActive = true AND a.isDeleted = false")
    Optional<AhpWeightSet> findActive();

    /** Returns the highest existing version number, or empty if no version yet. */
    Optional<AhpWeightSet> findTopByOrderByVersionDesc();

    /** Lists every version, newest first. */
    List<AhpWeightSet> findAllByOrderByVersionDesc();
}
