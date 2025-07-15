package software.mindware.taskmaster.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "TaskMaster API",
                version = "1.0",
                description = "API for managing challenges, team authentication, and submissions for TaskMaster competitions.",
                contact = @Contact(
                        name = "Michel Bijnen",
                        email = "info@michelbijnen.nl",
                        url = "https://www.michelbijnen.nl/"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        tags = {
                @Tag(name = "Team operations", description = "Endpoints related to team registration and authentication"),
                @Tag(name = "Team challenges", description = "Endpoints related to challenges a team can use"),
                @Tag(name = "Administration", description = "Endpoints for admin tasks like creating challenges")
        }
)
@Configuration
public class OpenApiConfig {
}
