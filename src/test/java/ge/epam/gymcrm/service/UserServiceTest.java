package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.UserDAO;
import ge.epam.gymcrm.domain.User;
import ge.epam.gymcrm.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        userService.setUserDAO(userDAO);
        userService.setPasswordEncoder(passwordEncoder);
    }

    private User user(String username, String rawPassword) {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
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
    void createUserStoresAHashButExposesTheRawPasswordOnce() {
        when(userDAO.findUsernamesStartingWith(anyString())).thenReturn(List.of());

        User created = userService.createUser("John", "Doe");

        assertThat(created.getRawPassword()).hasSize(10).matches("[A-Za-z0-9]+");
        assertThat(created.getPassword()).isNotEqualTo(created.getRawPassword());
        assertThat(passwordEncoder.matches(created.getRawPassword(), created.getPassword())).isTrue();
    }

    @Test
    void changePasswordStoresTheNewHashedPassword() {
        User stored = user("John.Doe", "oldPassword");
        when(userDAO.findByUsername("John.Doe")).thenReturn(Optional.of(stored));

        userService.changePassword("John.Doe", "oldPassword", "newPassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDAO).update(captor.capture());
        assertThat(passwordEncoder.matches("newPassword", captor.getValue().getPassword())).isTrue();
    }

    @Test
    void changePasswordFailsWhenOldPasswordIsWrong() {
        when(userDAO.findByUsername("John.Doe")).thenReturn(Optional.of(user("John.Doe", "oldPassword")));

        assertThatThrownBy(() -> userService.changePassword("John.Doe", "wrong", "newPassword"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void changePasswordFailsForUnknownUser() {
        when(userDAO.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword("Nobody", "old", "newPassword"))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
