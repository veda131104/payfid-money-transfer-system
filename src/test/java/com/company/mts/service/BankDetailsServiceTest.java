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
}
