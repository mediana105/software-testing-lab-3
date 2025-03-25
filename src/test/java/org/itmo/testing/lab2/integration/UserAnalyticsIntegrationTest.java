package org.itmo.testing.lab2.integration;

import io.javalin.Javalin;
import io.restassured.RestAssured;
import org.itmo.testing.lab2.controller.UserAnalyticsController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAnalyticsIntegrationTest {

    private Javalin app;

    @BeforeAll
    void setUp() {
        app = UserAnalyticsController.createApp();
        int port = 7000;
        app.start(port);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterAll
    void tearDown() {
        app.stop();
    }

    static Stream<Arguments> provideRegisterData() {
        return Stream.of(
                Arguments.of("user123", "John Doe", 200, "User registered: true"), // успешная регистрация пользователя user123
                Arguments.of("user456", "John Doe", 200, "User registered: true"), // успешная регистрация пользователя user456
                Arguments.of("user123", "John", 409, "User already exists"), // попытка зарегистрировать пользователя с уже существующим id
                Arguments.of("", null, 400, "Missing parameters"), // попытка зарегистрировать пользователя без id
                Arguments.of(null, "", 400, "Missing parameters") // попытка зарегистрировать пользователя без имени
        );
    }

    @ParameterizedTest
    @MethodSource("provideRegisterData")
    @Order(1)
    @DisplayName("Тест регистрации пользователя")
    void testUserRegistration(String userId, String userName, int expectedStatusCode, String expectedResponse) {
        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            params.put("userId", userId);
        }
        if (userName != null) {
            params.put("userName", userName);
        }
        given()
                .queryParams(params)
                .when()
                .post("/register")
                .then()
                .statusCode(expectedStatusCode)
                .body(equalTo(expectedResponse));
    }


    static Stream<Arguments> provideRecordSessionData() {
        return Stream.of(
                Arguments.of("user123", "2025-02-18T10:00:00", "2025-02-18T12:00:00", 200, "Session recorded"), // успешная запись сессии для пользователя user123
                Arguments.of("user123", "2025-02-18T10:00:00", "2025-02-18T10:00:00", 200, "Session recorded"), // успешная запись сессии для пользователя user123 с равными loginTime и logoutTime
                Arguments.of("user123", "2025-02-18T12:00:00", "2025-02-18T10:00:00", 400, "Login Time must be not not later than Logout Time"), // попытка создать запись, в которой loginTime больше logoutTime
                Arguments.of("user789", "2025-02-18T10:00:00", "2025-02-18T12:00:00", 400, "Invalid data: User not found"), // попытка создать запись для несуществующего пользователя
                Arguments.of(null, "2025-02-18T10:00:00", "2025-02-18T12:00:00", 400, "Missing parameters"), // попытка создать запись без id
                Arguments.of("user123", null, "2025-02-18T12:00:00", 400, "Missing parameters"), // попытка создать запись без loginTime
                Arguments.of("user123", "2025-02-18T10:00:00", null, 400, "Missing parameters"), // попытка создать запись без logoutTime
                Arguments.of("user123", "2025-01-18T10:00:00", "2025-02-18T12:00:00", 200, "Session recorded"), // успешная запись сессии с разными месяцами у loginTime и logoutTime
                Arguments.of("user123", "2025-01-18-10:00:00", "2025-02-18T12:00:00", 400, "Invalid data: Text '2025-01-18-10:00:00' could not be parsed at index 10") // попытка создать запись с некорректным форматом даты
        );
    }


    @ParameterizedTest
    @MethodSource("provideRecordSessionData")
    @Order(2)
    @DisplayName("Тест записи сессии")
    void testRecordSession(String userId, String loginTime, String logoutTime, int expectedStatusCode, String expectedResponse) {
        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            params.put("userId", userId);
        }
        if (loginTime != null) {
            params.put("loginTime", loginTime);
        }
        if (logoutTime != null) {
            params.put("logoutTime", logoutTime);
        }
        given()
                .queryParams(params)
                .when()
                .post("/recordSession")
                .then()
                .statusCode(expectedStatusCode)
                .body(equalTo(expectedResponse));
    }

    static Stream<Arguments> provideTotalActivity() {
        return Stream.of(
                Arguments.of("user123", 200, "Total activity: 44760 minutes"), // успешное получение данных об общей активности user123
                Arguments.of("user456", 404, "No sessions found for user"), // попытка получить данные об общей активности у пользователя без сессий
                Arguments.of(null, 400, "Missing userId") // попытка получить данные об общей активности без id
        );
    }

    @ParameterizedTest
    @MethodSource("provideTotalActivity")
    @Order(3)
    @DisplayName("Тест получения общего времени активности")
    void testGetTotalActivity(String userId, int expectedStatusCode, String expectedResponse) {
        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            params.put("userId", userId);
        }
        given()
                .queryParams(params)
                .when()
                .get("/totalActivity")
                .then()
                .statusCode(expectedStatusCode)
                .body(equalTo(expectedResponse));
    }

    static Stream<Arguments> provideInactiveUsers() {
        return Stream.of(
                Arguments.of(null, 400, "Missing days parameter"), // попытка получить информацию о неактивных пользователях без параметра дни
                Arguments.of("100", 200, "[]"), // успешное получение информации о неактивных пользователях, возвращаемое значение -- пустой лист
                Arguments.of("2", 200, "[\"user123\"]"), // успешное получение информации о неактивных пользователях, возвращаемое значение -- непустой лист
                Arguments.of("-1", 400, "The number of days must be non-negative"), // попытка получить информацию о неактивных пользователях с отрицательным числом дней
                Arguments.of("0", 200, "[\"user123\"]"), // попытка получить информацию о неактивных пользователях с нулевым числом дней
                Arguments.of("1", 200, "[\"user123\"]"),  // попытка получить информацию о неактивных пользователях с положительным числом дней
                Arguments.of("aaa", 400, "Invalid number format for days")  // попытка получить информацию о неактивных пользователях с некорректным форматом параметра дни
        );
    }

    @ParameterizedTest
    @MethodSource("provideInactiveUsers")
    @Order(4)
    @DisplayName("Тест получения неактивных пользователей")
    void testGetInactiveUsers(String days, int expectedStatusCode, String expectedResponse) {
        Map<String, Object> params = new HashMap<>();
        if (days != null) {
            params.put("days", days);
        }
        given()
                .queryParams(params)
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(expectedStatusCode)
                .body(equalTo(expectedResponse));
    }

    static Stream<Arguments> provideMonthlyActivity() {
        return Stream.of(
                Arguments.of("user789", null, 400, "Missing parameters"), // попытка получить информацию о месячной активности с нулевым параметром месяц
                Arguments.of("user123", "2025-03", 200, "{}"), // успешное получение информации о месячной активности, возвращаемое значение -- пустой словарь
                Arguments.of("user123", "2025-02", 200, "{\"2025-02-18\":240}"), // успешное получение информации о месячной активности, возвращаемое значение -- непустой словарь
                Arguments.of("user123", "2024-02", 200, "{}"), // успешное получение информации о месячной активности, с разным годом, но одинаковым месяцем
                Arguments.of(null, "2025-02", 400, "Missing parameters"), // попытка получить информацию о месячной активности с нулевым id
                Arguments.of("user123", "aaaa", 400, "Invalid data: Text 'aaaa' could not be parsed at index 0"), // попытка получить информацию о неактивных пользователях с некорректным форматом параметра месяц
                Arguments.of("user123", "2025-13", 400, "Invalid data: Text '2025-13' could not be parsed:" +
                        " Unable to obtain YearMonth from TemporalAccessor: {MonthOfYear=13, Year=2025},ISO of type java.time.format.Parsed") // попытка получить информацию о неактивных пользователях с некорректным форматом параметра месяц
        );
    }

    @ParameterizedTest
    @MethodSource("provideMonthlyActivity")
    @Order(5)
    @DisplayName("Тест получения месячной активности")
    void testGetMonthlyActivity(String userId, String month, int expectedStatusCode, String expectedResponse) {
        Map<String, Object> params = new HashMap<>();
        if (userId != null) {
            params.put("userId", userId);
        }
        if (month != null) {
            params.put("month", month);
        }
        given()
                .queryParams(params)
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(expectedStatusCode)
                .body(equalTo(expectedResponse));
    }
}
