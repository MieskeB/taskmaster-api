package software.mindware.taskmaster;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.mindware.taskmaster.dto.AuthRequest;
import software.mindware.taskmaster.model.Challenge;
import software.mindware.taskmaster.model.Submission;
import software.mindware.taskmaster.model.Team;
import software.mindware.taskmaster.repository.ChallengeRepository;
import software.mindware.taskmaster.repository.SubmissionRepository;
import software.mindware.taskmaster.repository.TeamRepository;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class Controller {
    private final ChallengeRepository challengeRepository;
    private final SubmissionRepository submissionRepository;
    private final TeamRepository teamRepository;

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${admin.code}")
    private String adminCode;

    public Controller(ChallengeRepository challengeRepository, SubmissionRepository submissionRepository, TeamRepository teamRepository) {
        this.challengeRepository = challengeRepository;
        this.submissionRepository = submissionRepository;
        this.teamRepository = teamRepository;
    }

    @Operation(summary = "Create a new team", tags = {"Administration"})
    @ApiResponse(responseCode = "200", description = "Team created successfully")
    @ApiResponse(responseCode = "403", description = "Invalid admin code")
    @PostMapping("/team")
    public ResponseEntity<String> createTeam(
            @Parameter(description = "Admin code", schema = @Schema(type = "string", format = "password")) String adminCode,
            @Parameter(description = "Team name") String teamName,
            @Parameter(description = "Team code") String code) {
        if (!this.adminCode.equals(adminCode)) {
            return ResponseEntity.status(403).body("{}");
        }
        Team team = new Team();
        team.setTeamName(teamName);
        team.setCode(code);
        teamRepository.save(team);
        return ResponseEntity.ok("{}");
    }

    @Operation(summary = "Get all teams", tags = {"Administration"})
    @ApiResponse(responseCode = "200", description = "List of teams")
    @ApiResponse(responseCode = "403", description = "Invalid admin code")
    @GetMapping("/team")
    public ResponseEntity<?> getAllTeams(
            @Parameter(description = "Admin code", schema = @Schema(type = "string", format = "password")) String adminCode
    ) {
        if (!this.adminCode.equals(adminCode)) {
            return ResponseEntity.status(403).body("{}");
        }
        List<Team> teams = teamRepository.findAll();
        return ResponseEntity.ok(teams);
    }

    @Operation(summary = "Authenticate a team and receive a token", tags = {"Team operations"})
    @ApiResponse(responseCode = "200", description = "Authenticated successfully")
    @ApiResponse(responseCode = "403", description = "Authentication failed")
    @PostMapping("/authenticate")
    public ResponseEntity<String> authenticate(@RequestBody AuthRequest authRequest) {
        Optional<Team> optionalTeam = this.teamRepository.findFirstByTeamName(authRequest.getTeamName());
        if (optionalTeam.isEmpty()) {
            return ResponseEntity.status(403).body("{}");
        }
        Team team = optionalTeam.get();
        if (!team.getCode().equals(authRequest.getCode())) {
            return ResponseEntity.status(403).body("{}");
        }
        String token = UUID.randomUUID().toString();
        team.getAuthenticationCodes().add(token);
        this.teamRepository.save(team);
        token = team.getTeamName() + "_" + token;
        return ResponseEntity.ok("{\"token\": \"" + token + "\"}");
    }

    @Operation(summary = "Submit a file for the current challenge", tags = {"Team challenges"})
    @ApiResponse(responseCode = "200", description = "File uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @ApiResponse(responseCode = "403", description = "Authentication failed")
    @PostMapping(value = "/submission", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(
            @Parameter(description = "Authentication token") @RequestParam("token") String token,
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file) {
        if (!token.contains("_")) {
            return ResponseEntity.status(403).body("{}");
        }
        String teamName = token.split("_")[0];
        String authCode = token.split("_")[1];
        Optional<Team> optionalTeam = this.teamRepository.findFirstByTeamName(teamName);
        if (optionalTeam.isEmpty()) {
            return ResponseEntity.status(403).body("{}");
        }
        Team team = optionalTeam.get();
        if (!team.getAuthenticationCodes().contains(authCode)) {
            return ResponseEntity.status(403).body("{}");
        }

        Optional<Challenge> optionalCurrentChallenge = this.challengeRepository.findFirstByStartDateBeforeOrderByStartDateDesc(Instant.now());
        if (optionalCurrentChallenge.isEmpty()) {
            return ResponseEntity.status(500).body("{}");
        }
        Challenge currentChallenge = optionalCurrentChallenge.get();

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType)) {
            return ResponseEntity.badRequest().body("{\"error\": \"Only image and video files are allowed.\"}");
        }

        try {
            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null && originalFileName.contains(".") ? originalFileName.substring(originalFileName.indexOf(".")) : "";
            String fileName = teamName + "_" + currentChallenge.getId() + "_" + UUID.randomUUID() + extension;
            Path filePath = dirPath.resolve(fileName);

            file.transferTo(filePath.toFile());

            Submission submission = new Submission();
            submission.setTeam(team);
            submission.setChallenge(currentChallenge);
            submission.setUploadedAt(Instant.now());
            submission.setFileName(fileName);
            submissionRepository.save(submission);

            return ResponseEntity.ok("{}");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{}");
        }
    }

    @Operation(summary = "List all challenges that have started", tags = {"Team challenges"})
    @ApiResponse(responseCode = "200", description = "List of challenges")
    @GetMapping("/challenge")
    public ResponseEntity<List<Challenge>> getChallenges() {
        List<Challenge> challenges = challengeRepository.findAllByStartDateBeforeOrderByStartDateDesc(Instant.now());
        return ResponseEntity.ok(challenges);
    }

    @Operation(summary = "Get the current active challenge", tags = {"Team challenges"})
    @ApiResponse(responseCode = "200", description = "Current challenge")
    @ApiResponse(responseCode = "500", description = "No challenge currently active")
    @GetMapping("/challenge/current")
    public ResponseEntity<?> getChallenge() {
        Optional<Challenge> optionalCurrentChallenge = this.challengeRepository.findFirstByStartDateBeforeOrderByStartDateDesc(Instant.now());
        if (optionalCurrentChallenge.isEmpty()) {
            return ResponseEntity.status(500).body("{}");
        }
        Challenge currentChallenge = optionalCurrentChallenge.get();
        return ResponseEntity.ok(currentChallenge);
    }

    @Operation(summary = "Create a new challenge", tags = {"Administration"})
    @ApiResponse(responseCode = "200", description = "Challenge created")
    @ApiResponse(responseCode = "403", description = "Invalid admin code")
    @PostMapping("/challenge/create")
    public ResponseEntity<String> createChallenge(
            @Parameter(description = "Admin code", schema = @Schema(type = "string", format = "password")) String adminCode,
            @Parameter(description = "Challenge title") String title,
            @Parameter(description = "Challenge description") String description,
            @Parameter(description = "Start date (e.g. 2025-06-03T15:00:00Z (UTC))") Instant startDate) {
        if (!this.adminCode.equals(adminCode)) {
            return ResponseEntity.status(403).body("{}");
        }
        Challenge challenge = new Challenge();
        challenge.setTitle(title);
        challenge.setDescription(description);
        challenge.setStartDate(startDate);
        this.challengeRepository.save(challenge);
        return ResponseEntity.ok("{}");
    }

    @Operation(summary = "Download all submissions for a challenge", tags = {"Administration"})
    @ApiResponse(responseCode = "200", description = "ZIP file with submissions")
    @ApiResponse(responseCode = "204", description = "No submissions")
    @ApiResponse(responseCode = "403", description = "Invalid admin code")
    @GetMapping("/challenge/{challengeId}/submissions")
    public ResponseEntity<Resource> downloadSubmissionsZip(
            @Parameter(description = "Challenge ID") @PathVariable Long challengeId,
            @Parameter(description = "Admin code", schema = @Schema(type = "string", format = "password")) @RequestParam String adminCode) {
        if (!this.adminCode.equals(adminCode)) {
            return ResponseEntity.status(403).build();
        }

        Optional<Challenge> optionalChallenge = challengeRepository.findById(challengeId);
        if (optionalChallenge.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Submission> submissions = optionalChallenge.get().getSubmissions();
        if (submissions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        try {
            Path zipPath = Files.createTempFile("submissions_" + challengeId + "_", ".zip");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                for (Submission submission : submissions) {
                    Path filePath = Paths.get(uploadDir).resolve(submission.getFileName()).normalize();
                    if (Files.exists(filePath)) {
                        zos.putNextEntry(new ZipEntry(submission.getFileName()));
                        Files.copy(filePath, zos);
                        zos.closeEntry();
                    }
                }
            }

            Resource resource = new FileSystemResource(zipPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=submissions_challenge_" + challengeId + ".zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "Delete all submissions for a challenge from a team", tags = {"Administration"})
    @ApiResponse(responseCode = "200", description = "Submissions deleted")
    @ApiResponse(responseCode = "403", description = "Invalid admin code")
    @DeleteMapping("/challenge/{challengeId}/submissions/{teamId}")
    public ResponseEntity<String> deleteTeamSubmissionsForChallenge(
            @PathVariable Long challengeId,
            @PathVariable Long teamId,
            @Parameter(schema = @Schema(type = "string", format = "password")) @RequestParam String adminCode) {

        if (!this.adminCode.equals(adminCode)) {
            return ResponseEntity.status(403).body("{}");
        }

        Optional<Team> optionalTeam = teamRepository.findById(teamId);
        Optional<Challenge> optionalChallenge = challengeRepository.findById(challengeId);

        if (optionalTeam.isEmpty() || optionalChallenge.isEmpty()) {
            return ResponseEntity.badRequest().body("{}");
        }

        Team team = optionalTeam.get();
        Challenge challenge = optionalChallenge.get();

        List<Submission> submissions = submissionRepository.findAllByTeamAndChallenge(team, challenge);
        for (Submission submission : submissions) {
            Path filePath = Paths.get(uploadDir).resolve(submission.getFileName()).normalize();
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            submissionRepository.delete(submission);
        }

        return ResponseEntity.ok("{}");
    }

    @Operation(summary = "Delete a challenge and its submissions", tags = {"Administration"})
    @ApiResponse(responseCode = "200", description = "Challenge deleted")
    @ApiResponse(responseCode = "403", description = "Invalid admin code")
    @DeleteMapping("/challenge/{challengeId}")
    public ResponseEntity<String> deleteChallenge(
            @PathVariable Long challengeId,
            @Parameter(schema = @Schema(type = "string", format = "password")) @RequestParam String adminCode) {

        if (!this.adminCode.equals(adminCode)) {
            return ResponseEntity.status(403).body("{}");
        }

        Optional<Challenge> optionalChallenge = challengeRepository.findById(challengeId);
        if (optionalChallenge.isEmpty()) {
            return ResponseEntity.badRequest().body("{}");
        }

        Challenge challenge = optionalChallenge.get();
        List<Submission> submissions = challenge.getSubmissions();
        for (Submission submission : submissions) {
            Path filePath = Paths.get(uploadDir).resolve(submission.getFileName()).normalize();
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            submissionRepository.delete(submission);
        }

        challengeRepository.delete(challenge);
        return ResponseEntity.ok("{}");
    }

    @Operation(summary = "Get all challenges (admin-only)", tags = {"Administration"})
    @ApiResponse(responseCode = "200", description = "List of all challenges")
    @ApiResponse(responseCode = "403", description = "Invalid admin code")
    @GetMapping("/challenge/all")
    public ResponseEntity<?> getAllChallengesAdmin(
            @Parameter(schema = @Schema(type = "string", format = "password")) @RequestParam String adminCode) {
        if (!this.adminCode.equals(adminCode)) {
            return ResponseEntity.status(403).body("{}");
        }
        List<Challenge> challenges = challengeRepository.findAll();
        return ResponseEntity.ok(challenges);
    }

    private boolean isAllowedType(String contentType) {
        return contentType.startsWith("image/") || contentType.startsWith("video/");
    }
}
