package com.Chanalyst.ChanalystV1.Repository;

import com.Chanalyst.ChanalystV1.Entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByCode(String code);
}