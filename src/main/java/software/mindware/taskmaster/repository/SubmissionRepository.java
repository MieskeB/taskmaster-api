package software.mindware.taskmaster.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import software.mindware.taskmaster.model.Submission;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
}
