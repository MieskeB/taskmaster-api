package software.mindware.taskmaster.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Authentication payload request")
public class AuthRequest {
    @Schema(description = "Team name", example = "teamA")
    private String teamName;
    @Schema(description = "Authentication code", example = "1234")
    private String code;
}
