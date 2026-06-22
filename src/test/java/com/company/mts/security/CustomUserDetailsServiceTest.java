package com.company.mts.security;

import com.company.mts.entity.AuthUser;
import com.company.mts.repository.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private AuthUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AuthUser();
        testUser.setId(10L);
        testUser.setName("bob");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void loadUserByUsername_Success() {
        when(authUserRepository.findByNameIgnoreCase("bob")).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserByUsername("bob");

        assertNotNull(result);
        assertEquals("bob", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
    }

    @Test
    void loadUserByUsername_NotFound_ThrowsException() {
        when(authUserRepository.findByNameIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
    }

    @Test
    void loadUserById_Success() {
        when(authUserRepository.findById(10L)).thenReturn(Optional.of(testUser));

        UserDetails result = service.loadUserById(10L);

        assertNotNull(result);
        assertEquals("bob", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
    }

    @Test
    void loadUserById_NotFound_ThrowsException() {
        when(authUserRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserById(999L));
    }
}
