package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.UserDAO;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.exception.AuthenticationFailedException;
import ge.epam.gymcrm.metrics.GymMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private GymMetrics metrics;

    @InjectMocks
    private UserService userService;

    private User user(String username, String password) {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername(username);
        user.setPassword(password);
        user.setActive(true);
        return user;
    }

    @Test
    void createUserGeneratesFirstDotLastUsernameWhenFree() {
        when(userDAO.findUsernamesStartingWith("John.Doe")).thenReturn(List.of());

        User created = userService.createUser("John", "Doe");

        assertThat(created.getUsername()).isEqualTo("John.Doe");
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void createUserAppendsSerialNumberWhenUsernameIsTaken() {
        when(userDAO.findUsernamesStartingWith("John.Doe"))
                .thenReturn(List.of("John.Doe", "John.Doe1"));

        User created = userService.createUser("John", "Doe");

        assertThat(created.getUsername()).isEqualTo("John.Doe2");
    }

    @Test
    void createUserGeneratesTenCharacterPassword() {
        when(userDAO.findUsernamesStartingWith(anyString())).thenReturn(List.of());

        User created = userService.createUser("John", "Doe");

        assertThat(created.getPassword()).hasSize(10).matches("[A-Za-z0-9]+");
    }

    @Test
    void authenticateReturnsUserWhenPasswordMatches() {
        when(userDAO.findByUsername("John.Doe")).thenReturn(Optional.of(user("John.Doe", "secret")));

        User authenticated = userService.authenticate("John.Doe", "secret");

        assertThat(authenticated.getUsername()).isEqualTo("John.Doe");
    }

    @Test
    void authenticateFailsOnWrongPassword() {
        when(userDAO.findByUsername("John.Doe")).thenReturn(Optional.of(user("John.Doe", "secret")));

        assertThatThrownBy(() -> userService.authenticate("John.Doe", "wrong"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void authenticateFailsOnUnknownUser() {
        when(userDAO.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.authenticate("Nobody", "secret"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void changePasswordStoresTheNewPassword() {
        User stored = user("John.Doe", "oldPassword");
        when(userDAO.findByUsername("John.Doe")).thenReturn(Optional.of(stored));

        userService.changePassword("John.Doe", "oldPassword", "newPassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDAO).update(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("newPassword");
    }

    @Test
    void changePasswordFailsWhenOldPasswordIsWrong() {
        when(userDAO.findByUsername("John.Doe")).thenReturn(Optional.of(user("John.Doe", "oldPassword")));

        assertThatThrownBy(() -> userService.changePassword("John.Doe", "wrong", "newPassword"))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
