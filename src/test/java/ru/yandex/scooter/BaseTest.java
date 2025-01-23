package ru.yandex.scooter;

import io.restassured.RestAssured;
import org.junit.BeforeClass;
import io.qameta.allure.Step;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class BaseTest {
    protected static final String BASE_URI = "http://qa-scooter.praktikum-services.ru";
    protected String courierLogin;
    protected String courierPassword;
    protected String courierFirstName;

    @BeforeClass
    public static void setup() {
        RestAssured.baseURI = BASE_URI;
    }

    @Step("Генерация данных курьера для теста")
    public void generateCourierData() {
        courierLogin = "courier" + System.currentTimeMillis();
        courierPassword = "password123";
        courierFirstName = "TestCourier";
    }

    @Step("Создание курьера с логином: {login}, паролем: {password}, именем: {firstName}")
    protected int createCourier(String login, String password, String firstName) {
        sendCreateCourierRequest(login, password, firstName)
                .statusCode(201)
                .body("ok", is(true));

        return loginCourier(login, password);
    }

    @Step("Отправка запроса на создание курьера")
    protected io.restassured.response.ValidatableResponse sendCreateCourierRequest(String login, String password, String firstName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("login", login);
        if (password != null) {
            requestBody.put("password", password);
        }
        if (firstName != null) {
            requestBody.put("firstName", firstName);
        }

        return given()
                .header("Content-type", "application/json")
                .body(requestBody)
                .when()
                .post("/api/v1/courier")
                .then();
    }

    @Step("Авторизация курьера с логином: {login} и паролем: {password}")
    protected int loginCourier(String login, String password) {
        return sendLoginCourierRequest(login, password)
                .statusCode(200)
                .extract()
                .path("id");
    }

    @Step("Отправка запроса на авторизацию курьера")
    protected io.restassured.response.ValidatableResponse sendLoginCourierRequest(String login, String password) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("login", login);
        if (password != null) {
            requestBody.put("password", password);
        }

        return given()
                .header("Content-type", "application/json")
                .body(requestBody)
                .when()
                .post("/api/v1/courier/login")
                .then();
    }

    @Step("Удаление курьера с ID: {courierId}")
    protected void deleteCourier(int courierId) {
        sendDeleteCourierRequest(courierId)
                .statusCode(200)
                .body("ok", is(true));
    }

    @Step("Отправка запроса на удаление курьера")
    protected io.restassured.response.ValidatableResponse sendDeleteCourierRequest(int courierId) {
        return given()
                .when()
                .delete("/api/v1/courier/" + courierId)
                .then();
    }

    @Step("Создание заказа")
    protected int createOrder(Map<String, Object> orderData) {
        return given()
                .header("Content-type", "application/json")
                .body(orderData)
                .when()
                .post("/api/v1/orders")
                .then()
                .statusCode(201)
                .extract()
                .path("track");
    }

    @Step("Получение ID заказа по трек-номеру {track}")
    protected int getOrderIdByTrack(int track) {
        return given()
                .queryParam("t", track)
                .when()
                .get("/api/v1/orders/track")
                .then()
                .statusCode(200)
                .extract()
                .path("order.id");
    }

    @Step("Курьер {courierId} принимает заказ {orderId}")
    protected void acceptOrder(int orderId, int courierId) {
        given()
                .queryParam("courierId", courierId)
                .when()
                .put("/api/v1/orders/accept/" + orderId)
                .then()
                .statusCode(200)
                .body("ok", is(true));
    }
}
