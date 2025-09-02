package com.api.staff_manager.services;

import com.api.staff_manager.exceptions.custom.UserNotFoundException;
import com.api.staff_manager.mappers.UserMapper;
import com.api.staff_manager.models.UserModel;
import com.api.staff_manager.repositories.UserRepository;
import com.api.staff_manager.services.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Load user by username with existing email returns UserDetails")
    void givenExistingUserEmail_whenLoadUserByUsername_thenReturnUserDetails() {
        String userEmail = "john.doe@example.com";

        var user = new UserModel();
        user.setEmail(userEmail);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

        UserDetails returnedUser = userService.loadUserByUsername(userEmail);

        assertNotNull(returnedUser);
        assertEquals(userEmail, returnedUser.getUsername());
        verify(userRepository, times(1)).findByEmail(userEmail);
    }

    @Test
    @DisplayName("Load user by username with non-existing email throws UserNotFoundException")
    void givenNonExistingUserEmail_whenLoadUserByUsername_thenThrowUserNotFoundException() {
        String userEmail = "john.doe@example.com";

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        var exception = assertThrows(UserNotFoundException.class, () -> userService.loadUserByUsername(userEmail));

        assertEquals("User not found with email: " + userEmail, exception.getMessage());

        verify(userRepository, times(1)).findByEmail(userEmail);
    }
}