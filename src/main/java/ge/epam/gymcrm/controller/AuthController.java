package ge.epam.gymcrm.controller;

import ge.epam.gymcrm.dto.request.ChangePasswordRequest;
import ge.epam.gymcrm.dto.response.ErrorResponse;
import ge.epam.gymcrm.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Authentication", description = "Login and password management")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Login",
            description = "Checks that the username and password match. No credentials headers needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials are valid",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Username or password missing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Username and password do not match",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/login")
    public ResponseEntity<Void> login(
            @Parameter(description = "Username", required = true, example = "John.Doe")
            @RequestParam @NotBlank(message = "Username is required") String username,

            @Parameter(description = "Password", required = true)
            @RequestParam @NotBlank(message = "Password is required") String password) {

        userService.authenticate(username, password);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Change login",
            description = "Replaces the password of the given user. The old password authenticates the call.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Username and old password do not match",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/login")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
