package software.mindware.taskmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CountResponse {
    private long teamId;
    private long challengeId;
    private long count;
}
