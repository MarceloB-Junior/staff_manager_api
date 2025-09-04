package com.api.staff_manager.services;

import com.api.staff_manager.dtos.requests.UserCreationRequest;
import com.api.staff_manager.dtos.responses.UserSummaryResponse;
import com.api.staff_manager.dtos.responses.UserViewResponse;
import com.api.staff_manager.enums.RoleEnum;
import com.api.staff_manager.exceptions.custom.UserExistsException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Test
    @DisplayName("Save valid user creation request returns UserSummaryResponse")
    void givenValidUserCreationRequest_whenSave_thenReturnsUserSummaryResponse() {
        var request = new UserCreationRequest("John Doe", "pwd123", "john.doe@example.com");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        var user = new UserModel();
        when(userMapper.toEntity(request)).thenReturn(user);

        var encodedPassword = "encoded.password";
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);

        var savedUser = new UserModel();
        savedUser.setUserId(UUID.randomUUID());
        savedUser.setEmail(request.email());
        savedUser.setRole(RoleEnum.USER);
        when(userRepository.save(user)).thenReturn(savedUser);

        var summaryResponse = new UserSummaryResponse(savedUser.getUserId(), "John Doe", savedUser.getEmail());
        when(userMapper.toSummaryResponse(savedUser)).thenReturn(summaryResponse);

        var result = userService.save(request);

        assertNotNull(result);
        assertEquals(summaryResponse.userId(), savedUser.getUserId());
        assertEquals(summaryResponse.email(), savedUser.getEmail());
        assertEquals(encodedPassword, user.getPassword());
        assertEquals(RoleEnum.USER, user.getRole());
        assertEquals(summaryResponse, result);

        verify(userRepository, times(1)).existsByEmail(request.email());
        verify(userMapper, times(1)).toEntity(request);
        verify(passwordEncoder, times(1)).encode(request.password());
        verify(userRepository, times(1)).save(user);
        verify(userMapper, times(1)).toSummaryResponse(savedUser);
    }

    @Test
    @DisplayName("Saving existing user email throws UserExistsException")
    void givenExistingUserEmail_whenSave_thenThrowsUserExistsException() {
        var request = new UserCreationRequest("John Doe", "pwd123", "john.doe@example.com");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        var exception = assertThrows(UserExistsException.class, () -> userService.save(request));

        assertEquals("A user with the provided email address already exists.", exception.getMessage());

        verify(userRepository, times(1)).existsByEmail(request.email());
        verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Find all method returns a page of UserViewResponse")
    void givenPageable_whenFindAll_thenReturnsPageOfUserViewResponse() {
        Pageable pageable = PageRequest.of(0, 10);

        var user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setRole(RoleEnum.ADMIN);

        var userViewResponse = new UserViewResponse(user.getUserId(), user.getName(), user.getEmail(), user.getRole());

        Page<UserModel> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toViewResponse(user)).thenReturn(userViewResponse);

        Page<UserViewResponse> result = userService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(userViewResponse, result.getContent().getFirst());

        verify(userRepository,times(1)).findAll(pageable);
        verify(userMapper,times(1)).toViewResponse(user);
    }
}