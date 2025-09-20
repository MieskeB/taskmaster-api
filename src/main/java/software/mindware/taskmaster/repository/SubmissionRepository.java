package software.mindware.taskmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import software.mindware.taskmaster.model.Challenge;
import software.mindware.taskmaster.model.Submission;
import software.mindware.taskmaster.model.Team;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findAllByTeamAndChallenge(Team team, Challenge challenge);
    long countByTeamAndChallenge(Team team, Challenge challenge);
}
