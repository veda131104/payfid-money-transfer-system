package com.company.mts;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.mockito.Mockito.*;

class MainTest {

    @Test
    void testMainMethod() {
        try (MockedStatic<SpringApplication> springMock = mockStatic(SpringApplication.class)) {
            ConfigurableApplicationContext contextMock = mock(ConfigurableApplicationContext.class);
            ConfigurableEnvironment environmentMock = mock(ConfigurableEnvironment.class);

            when(contextMock.getEnvironment()).thenReturn(environmentMock);
            when(environmentMock.getProperty("server.port", "8080")).thenReturn("8080");
            when(environmentMock.getActiveProfiles()).thenReturn(new String[]{"test-profile"});
            when(environmentMock.getProperty("spring.datasource.url", "N/A")).thenReturn("jdbc:h2:mem:db");

            springMock.when(() -> SpringApplication.run(eq(Main.class), any(String[].class)))
                    .thenReturn(contextMock);

            Main.main(new String[]{"--arg1"});

            springMock.verify(() -> SpringApplication.run(eq(Main.class), any(String[].class)), times(1));
        }
    }
}
