package software.mindware.taskmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import software.mindware.taskmaster.model.Challenge;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    Optional<Challenge> findFirstByStartDateBeforeOrderByStartDateDesc(Instant now);
    List<Challenge> findAllByStartDateBeforeOrderByStartDateDesc(Instant now);
}
