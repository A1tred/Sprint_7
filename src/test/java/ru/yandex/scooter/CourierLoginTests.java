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
import static io.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class CourierLoginTests extends BaseTest {

    private int courierId;

    // Параметры для параметризации
    private final String testDescription;
    private final String login;
    private final String password;
    private final int expectedStatusCode;
    private final String expectedMessage;

    public CourierLoginTests(String testDescription, String login, String password, int expectedStatusCode, String expectedMessage) {
        this.testDescription = testDescription;
        this.login = login;
        this.password = password;
        this.expectedStatusCode = expectedStatusCode;
        this.expectedMessage = expectedMessage;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getTestData() {
        return Arrays.asList(new Object[][]{
                {
                        "Успешная авторизация курьера",
                        "validLogin", "validPassword", 200, null
                },
                {
                        "Авторизация без логина",
                        null, "validPassword", 400, "Недостаточно данных для входа"
                },
                {
                        "Авторизация без пароля",
                        "validLogin", null, 400, "Недостаточно данных для входа"
                },
                {
                        "Авторизация с null логином",
                        "nullLogin", "validPassword", 400, "Недостаточно данных для входа"
                },
                {
                        "Авторизация с null паролем",
                        "validLogin", "nullPassword", 400, "Недостаточно данных для входа"
                },
                {
                        "Авторизация с неверным логином",
                        "wrongLogin", "validPassword", 404, "Учетная запись не найдена"
                },
                {
                        "Авторизация с неверным паролем",
                        "validLogin", "wrongPassword", 404, "Учетная запись не найдена"
                }
        });
    }

    @Before
    public void setUp() {
        generateCourierData();
        courierId = createCourier(courierLogin, courierPassword, courierFirstName);
    }

    @Test
    @DisplayName("Тест авторизации курьера")
    @Description("{0}")
    public void testCourierLogin() {
        // Подготовка данных для авторизации
        String loginToUse = getLoginValue(login);
        String passwordToUse = getPasswordValue(password);

        // Отправка запроса и проверка результата
        sendLoginRequestAndCheckResponse(loginToUse, passwordToUse, expectedStatusCode, expectedMessage);
    }

    @Step("Получение значения логина для теста")
    private String getLoginValue(String loginParam) {
        if ("validLogin".equals(loginParam)) {
            return courierLogin;
        } else if ("wrongLogin".equals(loginParam)) {
            return "nonExistentLogin";
        } else if ("nullLogin".equals(loginParam)) {
            return null;
        } else {
            return loginParam; // Для случаев явного указания значения
        }
    }

    @Step("Получение значения пароля для теста")
    private String getPasswordValue(String passwordParam) {
        if ("validPassword".equals(passwordParam)) {
            return courierPassword;
        } else if ("wrongPassword".equals(passwordParam)) {
            return "wrongPassword123";
        } else if ("nullPassword".equals(passwordParam)) {
            return null;
        } else {
            return passwordParam; // Для случаев явного указания значения
        }
    }

    @Step("Отправка запроса на авторизацию и проверка ответа")
    private void sendLoginRequestAndCheckResponse(String login, String password, int expectedStatusCode, String expectedMessage) {
        Map<String, Object> requestBody = new HashMap<>();
        if (login != null) {
            requestBody.put("login", login);
        }
        if (password != null) {
            requestBody.put("password", password);
        }

        var response = given()
                .header("Content-type", "application/json")
                .body(requestBody)
                .when()
                .post("/api/v1/courier/login")
                .then()
                .statusCode(expectedStatusCode);

        if (expectedStatusCode == 200) {
            int loginCourierId = response.extract().path("id");
            verifyCourierId(loginCourierId);
        } else if (expectedMessage != null) {
            response.body("message", equalTo(expectedMessage));
        }
    }

    @Step("Проверка соответствия ID курьера")
    public void verifyCourierId(int loginCourierId) {
        org.junit.Assert.assertEquals("Courier ID должен совпадать", courierId, loginCourierId);
    }

    @After
    public void tearDown() {
        if (courierId != 0) {
            deleteCourier(courierId);
        }
    }
}
