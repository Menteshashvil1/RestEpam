package ge.epam.gymcrm.controller;

import ge.epam.gymcrm.dto.request.ChangePasswordRequest;
import ge.epam.gymcrm.dto.request.LoginRequest;
import ge.epam.gymcrm.dto.response.ErrorResponse;
import ge.epam.gymcrm.dto.response.TokenResponse;
import ge.epam.gymcrm.service.AuthService;
import ge.epam.gymcrm.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Authentication", description = "Login, logout and password management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Autowired
    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @Operation(summary = "Login",
            description = "Authenticates username/password and returns a JWT bearer token. "
                    + "Send it as 'Authorization: Bearer <token>' on every other endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated — token returned"),
            @ApiResponse(responseCode = "400", description = "Username or password missing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Username and password do not match",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts — user blocked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        return new TokenResponse(request.username(), token);
    }

    @Operation(summary = "Logout",
            description = "Invalidates the presented bearer token. Handled by Spring Security's "
                    + "logout filter at this path.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Logged out", content = @Content))
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutDocumentationOnly() {

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
