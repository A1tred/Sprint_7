package ru.yandex.scooter;

import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.Description;
import io.qameta.allure.Step;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static io.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class CourierCreateTests extends BaseTest {

    private int courierId = 0;

    // Параметры для параметризации
    private final String testDescription;
    private final String login;
    private final String password;
    private final boolean includeLoginField;
    private final boolean includePasswordField;
    private final int expectedStatusCode;
    private final String expectedMessage;
    private final boolean isPositiveTest;

    public CourierCreateTests(String testDescription, String login, String password, boolean includeLoginField, boolean includePasswordField, int expectedStatusCode, String expectedMessage, boolean isPositiveTest) {
        this.testDescription = testDescription;
        this.login = login;
        this.password = password;
        this.includeLoginField = includeLoginField;
        this.includePasswordField = includePasswordField;
        this.expectedStatusCode = expectedStatusCode;
        this.expectedMessage = expectedMessage;
        this.isPositiveTest = isPositiveTest;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][]{
                {
                        "Создание курьера с логином и паролем",
                        "validLogin", "validPassword", true, true, 201, null, true
                },
                {
                        "Создание курьера без пароля",
                        "validLogin", null, true, false, 400, "Недостаточно данных для создания учетной записи", false
                },
                {
                        "Создание курьера без логина",
                        null, "validPassword", false, true, 400, "Недостаточно данных для создания учетной записи", false
                },
                {
                        "Создание курьера с паролем = null",
                        "validLogin", null, true, true, 400, "Недостаточно данных для создания учетной записи", false
                },
                {
                        "Создание курьера с логином = null",
                        null, "validPassword", true, true, 400, "Недостаточно данных для создания учетной записи", false
                },
        });
    }

    @Before
    public void setUp() {
        generateCourierData();
    }

    @After
    public void tearDown() {
        if (courierId != 0) {
            deleteCourier(courierId);
        }
    }

    @Test
    @DisplayName("Тест создания курьера с различными комбинациями данных")
    @Description("{0}")
    public void testCreatingCourierWithRequiredFields() {
        String loginToUse = getLoginValue(login);
        String passwordToUse = getPasswordValue(password);

        sendCreateCourierRequestAndCheckResponse(loginToUse, passwordToUse, courierFirstName, includeLoginField, includePasswordField, expectedStatusCode, expectedMessage, isPositiveTest);
    }

    @Step("Получение значения логина для теста")
    private String getLoginValue(String loginParam) {
        if ("validLogin".equals(loginParam)) {
            return courierLogin;
        } else {
            return loginParam; // Может быть null или явно указанное значение
        }
    }

    @Step("Получение значения пароля для теста")
    private String getPasswordValue(String passwordParam) {
        if ("validPassword".equals(passwordParam)) {
            return courierPassword;
        } else {
            return passwordParam; // Может быть null или явно указанное значение
        }
    }

    @Step("Отправка запроса на создание курьера и проверка ответа")
    private void sendCreateCourierRequestAndCheckResponse(String login, String password, String firstName, boolean includeLoginField, boolean includePasswordField, int expectedStatusCode, String expectedMessage, boolean isPositiveTest) {
        // Формирование тела запроса
        Map<String, Object> requestBody = new HashMap<>();
        if (includeLoginField) {
            requestBody.put("login", login);
        }
        if (includePasswordField) {
            requestBody.put("password", password);
        }
        // Поле firstName необязательное, добавляем всегда при наличии
        if (firstName != null) {
            requestBody.put("firstName", firstName);
        }

        // Отправка запроса и проверка ответа
        var response = given()
                .header("Content-type", "application/json")
                .body(requestBody)
                .when()
                .post("/api/v1/courier")
                .then()
                .statusCode(expectedStatusCode);

        if (isPositiveTest) {
            response.body("ok", is(true));
            // Если успешное создание, получаем ID курьера для удаления
            courierId = loginCourier(login, password);
        } else {
            if (expectedMessage != null) {
                response.body("message", equalTo(expectedMessage));
            }
        }
    }
}
