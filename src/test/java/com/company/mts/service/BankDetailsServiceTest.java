package com.company.mts.service;

import com.company.mts.entity.BankDetails;
import com.company.mts.repository.BankDetailsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankDetailsServiceTest {

    @Mock
    private BankDetailsRepository repository;

    @InjectMocks
    private BankDetailsService bankDetailsService;

    @Test
    void setupUpi_Success() {
        BankDetails details = new BankDetails();
        details.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(details));
        when(repository.save(any())).thenReturn(details);

        BankDetails result = bankDetailsService.setupUpi(1L, "test@upi");

        assertEquals("test@upi", result.getUpiId());
        verify(repository).save(details);
    }

    @Test
    void updatePin_Success() {
        BankDetails details = new BankDetails();
        details.setUserName("testuser");
        when(repository.findByUserName("testuser")).thenReturn(Optional.of(details));
        when(repository.save(any())).thenReturn(details);

        BankDetails result = bankDetailsService.updatePin("testuser", "1234");

        assertEquals("1234", result.getPin());
        verify(repository).save(details);
    }

    @Test
    void save_Success() {
        BankDetails details = new BankDetails();
        when(repository.save(details)).thenReturn(details);
        BankDetails result = bankDetailsService.save(details);
        assertNotNull(result);
        verify(repository).save(details);
    }

    @Test
    void findByAccountNumber_Found() {
        BankDetails details = new BankDetails();
        when(repository.findByAccountNumber("123")).thenReturn(Optional.of(details));
        Optional<BankDetails> result = bankDetailsService.findByAccountNumber("123");
        assertTrue(result.isPresent());
    }

    @Test
    void findByAccountNumber_NotFound() {
        when(repository.findByAccountNumber("123")).thenReturn(Optional.empty());
        Optional<BankDetails> result = bankDetailsService.findByAccountNumber("123");
        assertFalse(result.isPresent());
    }

    @Test
    void findById_Found() {
        BankDetails details = new BankDetails();
        when(repository.findById(1L)).thenReturn(Optional.of(details));
        Optional<BankDetails> result = bankDetailsService.findById(1L);
        assertTrue(result.isPresent());
    }

    @Test
    void findById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        Optional<BankDetails> result = bankDetailsService.findById(1L);
        assertFalse(result.isPresent());
    }

    @Test
    void findByUserName_Found() {
        BankDetails details = new BankDetails();
        when(repository.findByUserName("user")).thenReturn(Optional.of(details));
        Optional<BankDetails> result = bankDetailsService.findByUserName("user");
        assertTrue(result.isPresent());
    }

    @Test
    void findByUserName_NotFound() {
        when(repository.findByUserName("user")).thenReturn(Optional.empty());
        Optional<BankDetails> result = bankDetailsService.findByUserName("user");
        assertFalse(result.isPresent());
    }

    @Test
    void setupUpi_NotFound_ThrowsException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> bankDetailsService.setupUpi(1L, "upi"));
    }

    @Test
    void updatePin_NotFound_ThrowsException() {
        when(repository.findByUserName("user")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> bankDetailsService.updatePin("user", "1234"));
    }
}
