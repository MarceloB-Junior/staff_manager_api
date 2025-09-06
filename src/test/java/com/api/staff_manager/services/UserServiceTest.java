package com.api.staff_manager.services;

import com.api.staff_manager.dtos.requests.UserCreationRequest;
import com.api.staff_manager.dtos.requests.UserUpdateRequest;
import com.api.staff_manager.dtos.responses.UserDetailsResponse;
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

import java.time.LocalDateTime;
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

    @Test
    @DisplayName("Find by id method returns UserDetailsResponse when user exists")
    void givenExistingUserId_whenFindById_thenReturnsUserDetailsResponse() {
        UUID userId = UUID.randomUUID();

        var user = new UserModel();
        user.setUserId(userId);
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setRole(RoleEnum.ADMIN);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setUpdatedAt(LocalDateTime.now());

        var userDetailsResponse = new UserDetailsResponse(user.getUserId(), user.getName(), user.getEmail(),
                user.getRole(), user.getCreatedAt(), user.getUpdatedAt());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDetailsResponse(user)).thenReturn(userDetailsResponse);

        var response = userService.findById(userId);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(user.getName(), response.name());
        assertEquals(user.getEmail(), response.email());
        assertEquals(RoleEnum.ADMIN, response.role());

        verify(userRepository, times(1)).findById(userId);
        verify(userMapper, times(1)).toDetailsResponse(user);
    }

    @Test
    @DisplayName("Find by id method throws UserNotFoundException when user does not exist")
    void givenNonExistingUserId_whenFindById_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var exception = assertThrows(UserNotFoundException.class, () -> userService.findById(userId));

        assertEquals("User not found with id: " + userId, exception.getMessage());

        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Find by email method returns UserDetailsResponse when user exists")
    void givenExistingUserEmail_whenFindByEmail_thenReturnUserDetailsResponse() {
        String userEmail = "john.doe@example.com";

        var user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setName("John Doe");
        user.setEmail(userEmail);
        user.setRole(RoleEnum.ADMIN);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setUpdatedAt(LocalDateTime.now());

        var userDetailsResponse = new UserDetailsResponse(user.getUserId(), user.getName(), user.getEmail(),
                user.getRole(), user.getCreatedAt(), user.getUpdatedAt());

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(userMapper.toDetailsResponse(user)).thenReturn(userDetailsResponse);

        var response = userService.findByEmail(userEmail);

        assertNotNull(response);
        assertEquals(userEmail, response.email());
        assertEquals(userDetailsResponse, response);

        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(userMapper, times(1)).toDetailsResponse(user);
    }

    @Test
    @DisplayName("Find by email method throws UserNotFoundException when user does not exist")
    void givenNonExistingUserEmail_whenFindByEmail_thenThrowUserNotFoundException() {
        String userEmail = "john.doe@example.com";

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        var exception = assertThrows(UserNotFoundException.class, () -> userService.findByEmail(userEmail));

        assertEquals("User not found with email: " + userEmail, exception.getMessage());

        verify(userRepository, times(1)).findByEmail(userEmail);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Find model by email method returns UserModel when user exists")
    void givenExistingUserEmail_whenFindModelByEmail_thenReturnUserModel() {
        String userEmail = "john.doe@example.com";

        var user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setName("John Doe");
        user.setEmail(userEmail);
        user.setRole(RoleEnum.ADMIN);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

        var userModel = userService.findModelByEmail(userEmail);

        assertNotNull(userModel);
        assertEquals(user.getEmail(), userModel.getEmail());
        assertEquals(user, userModel);

        verify(userRepository, times(1)).findByEmail(userEmail);
    }

    @Test
    @DisplayName("Find model by email method throws UserNotFoundException when user does not exist")
    void givenNonExistingUserEmail_whenFindModelByEmail_thenThrowUserNotFoundException() {
        String userEmail = "john.doe@example.com";

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        var exception = assertThrows(UserNotFoundException.class, () -> userService.findByEmail(userEmail));

        assertEquals("User not found with email: " + userEmail, exception.getMessage());

        verify(userRepository, times(1)).findByEmail(userEmail);
    }

    @Test
    @DisplayName("Update existing user and valid user update request returns UserDetailsResponse")
    void givenExistingUserAndValidUserUpdateRequest_whenUpdate_thenReturnsUserDetailsResponse() {
        UUID userId = UUID.randomUUID();

        var request = new UserUpdateRequest("Mark Doe", "mark.doe@example.com", RoleEnum.USER);

        var existingUser = new UserModel();
        existingUser.setUserId(userId);
        existingUser.setEmail("john.doe@example.com");

        var updatedUser = new UserModel();
        updatedUser.setUserId(userId);
        updatedUser.setName(request.name());
        updatedUser.setEmail(request.email());
        updatedUser.setRole(request.role());
        updatedUser.setCreatedAt(LocalDateTime.now().minusDays(1));
        updatedUser.setUpdatedAt(LocalDateTime.now());

        var userDetailsResponse = new UserDetailsResponse(updatedUser.getUserId(), updatedUser.getName(),updatedUser.getEmail(),
                updatedUser.getRole(),updatedUser.getCreatedAt(),updatedUser.getUpdatedAt());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        /* The condition "userRepository.existsByEmail(request.email()) && !user.getEmail().equals(request.email())"
        is being mocked here to simplify this test */
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(existingUser)).thenReturn(updatedUser);
        when(userMapper.toDetailsResponse(updatedUser)).thenReturn(userDetailsResponse);

        var response = userService.update(request, userId);

        assertNotNull(response);
        assertEquals(userDetailsResponse, response);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).existsByEmail(request.email());
        verify(userRepository, times(1)).save(existingUser);
        verify(userMapper, times(1)).toDetailsResponse(updatedUser);
    }

    @Test
    @DisplayName("Update with non-existent user id throws UserNotFoundException")
    void givenNonExistingUserId_whenUpdate_thenThrowsUserNotFoundException() {
        UUID userId = UUID.randomUUID();

        var request = new UserUpdateRequest("John Doe", "john.doe@example.com", RoleEnum.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var exception = assertThrows(UserNotFoundException.class, () -> userService.update(request, userId));

        assertEquals("User not found with id: " + userId, exception.getMessage());

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Update existing user with email address from another user throws UserExistsException")
    void givenExistingEmailBelongingToAnotherUser_whenUpdate_thenThrowsUserExistsException() {
        UUID userId = UUID.randomUUID();

        var request = new UserUpdateRequest("John Doe", "john.doe@example.com", RoleEnum.USER);

        var existingUser = new UserModel();
        existingUser.setUserId(userId);
        existingUser.setEmail("mark.doe@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        /* The condition "userRepository.existsByEmail(request.email()) && !user.getEmail().equals(request.email())"
        is being mocked here to simplify this test */
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        var exception = assertThrows(UserExistsException.class, () -> userService.update(request, userId));

        assertEquals("A user with the provided email address already exists.", exception.getMessage());

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).existsByEmail(request.email());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper);
    }
}