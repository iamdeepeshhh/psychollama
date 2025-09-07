package com.Chanalyst.ChanalystV1.Repository;

import com.Chanalyst.ChanalystV1.Entity.Question;
import com.Chanalyst.ChanalystV1.Entity.Room;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByRoom(Room room);

    Optional<Question> findFirstByRoomAndUsedFalse(Room room);

    void deleteByRoom(Room room);

    @Query("SELECT q FROM Question q WHERE q.room = :room AND q.used = false ORDER BY function('RANDOM')")
    List<Question> findRandomUnused(Room room, Pageable pageable);

    List<Question> findByRoomAndRound(Room room, int round);

    // Deterministic ordering within a round to ensure all clients see the same sequence
    List<Question> findByRoomAndRoundOrderByIdAsc(Room room, int round);
    List<Question> findByRoomAndRoundOrderBySequenceAsc(Room room, int round);

    Optional<Question> findByRoomAndRoundAndSequence(Room room, int round, int sequence);

}
