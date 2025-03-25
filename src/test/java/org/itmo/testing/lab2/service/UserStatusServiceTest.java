package org.itmo.testing.lab2.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserStatusServiceTest {

    private UserAnalyticsService userAnalyticsService;
    private UserStatusService userStatusService;

    @BeforeEach
    void setUp() {
        userAnalyticsService = mock(UserAnalyticsService.class);
        userStatusService = new UserStatusService(userAnalyticsService);
    }


    @ParameterizedTest
    @CsvSource({
            "30, Inactive",   // Не граничное значение для Inactive
            "59, Inactive",   // Максимальное значение для Inactive
            "60, Active",     // Минимальное значение для Active
            "90, Active",     // Не граничное значение для Active
            "119, Active",    // Максимальное значение для Active
            "120, Highly active", // Минимальное значение для Highly active
            "200, Highly active"  // Не граничное значение для Highly active
    })
    @DisplayName("Тест получения статуса активности")
    public void testGetUserStatus(long activityTime, String expectedStatus) {
        when(userAnalyticsService.getTotalActivityTime("user123")).thenReturn(activityTime);

        String status = userStatusService.getUserStatus("user123");

        assertEquals(expectedStatus, status);
        verify(userAnalyticsService, times(1)).getTotalActivityTime("user123");
    }

    static Stream<Arguments> provideLastSessionData() {
        UserAnalyticsService.Session mockSession1 = mock(UserAnalyticsService.Session.class);
        when(mockSession1.getLogoutTime()).thenReturn(LocalDateTime.of(2024, 10, 10, 10, 0));

        UserAnalyticsService.Session mockSession2 = mock(UserAnalyticsService.Session.class);
        when(mockSession2.getLogoutTime()).thenReturn(LocalDateTime.of(2025, 2, 18, 12, 0));

        return Stream.of(
                // Получение последней сессии для пользователя с 1 сессией (граничный случай)
                Arguments.of(List.of(mockSession1), Optional.of("2024-10-10")),
                // Получение последней сессии для пользователя с более, чем 1 сессией в правильном порядке
                Arguments.of(List.of(mockSession1, mockSession2), Optional.of("2025-02-18")),
                // Получение последней сессии для пользователя с более, чем 1 сессией в неправильном порядке
                Arguments.of(List.of(mockSession2, mockSession1), Optional.of("2025-02-18")),
                // Получение последней сессии для пользователя без сессий
                Arguments.of(List.of(), Optional.empty())
        );
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("provideLastSessionData")
    @DisplayName("Тест получения последней записи")
    void testGetLastDate(ArgumentsAccessor params) {
        List<UserAnalyticsService.Session> sessions = params.get(0, List.class);
        Optional<String> expectedDate = params.get(1, Optional.class);
        when(userAnalyticsService.getUserSessions("user123")).thenReturn(sessions);
        Optional<String> actualDate = userStatusService.getUserLastSessionDate("user123");
        if (expectedDate.isEmpty()) {
            assertTrue(actualDate.isEmpty());
        } else {
            assertTrue(actualDate.isPresent());
            assertEquals(expectedDate, actualDate);
            verify(userAnalyticsService, times(1)).getUserSessions("user123");
        }
    }
}