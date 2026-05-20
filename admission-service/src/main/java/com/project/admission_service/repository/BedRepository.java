package com.project.admission_service.repository;

import com.project.admission_service.model.Bed;
import com.project.admission_service.model.BedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface BedRepository extends JpaRepository<Bed, UUID> {
    List<Bed> findByRoomIdAndStatus(UUID roomId, BedStatus status);

    /**
     * Atomically finds and locks one EMPTY bed in a specific ward.
     * SKIP LOCKED ensures that concurrent requests don't wait for each other;
     * they just grab the next available bed.
     */
    @Query(value = """
        SELECT b.* FROM admission_schema.beds b 
        JOIN admission_schema.rooms r ON b.room_id = r.id 
        WHERE r.ward_id = :wardId 
        AND b.status = 'EMPTY' 
        LIMIT 1 
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Bed> findFirstEmptyBedInWardForUpdate(@Param("wardId") UUID wardId);
}
