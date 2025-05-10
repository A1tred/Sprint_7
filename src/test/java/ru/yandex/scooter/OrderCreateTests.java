package ru.yandex.scooter;

import com.google.gson.Gson;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.Description;
import io.qameta.allure.Step;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.scooter.models.Order;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static io.restassured.RestAssured.given;

@RunWith(Parameterized.class)
public class OrderCreateTests extends BaseTest {

    private final List<String> color;

    public OrderCreateTests(List<String> color) {
        this.color = color;
    }

    @Parameterized.Parameters(name = "Цвет: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Arrays.asList("BLACK")},
                {Arrays.asList("GREY")},
                {Arrays.asList("BLACK", "GREY")},
                {Arrays.asList()}
        });
    }

    @Test
    @DisplayName("Создание заказа с различными вариантами цвета")
    @Description("Проверка создания заказа с разными вариантами цвета: BLACK, GREY, оба или без цвета")
    public void testCreateOrderWithDifferentColors() {
        Order order = createOrderObject(color);
        String jsonOrder = serializeOrder(order);
        sendCreateOrderRequest(jsonOrder)
                .statusCode(201)
                .body("track", notNullValue());
    }

    @Step("Создание объекта заказа с цветом: {color}")
    private Order createOrderObject(List<String> color) {
        return new Order(
                "Naruto",
                "Uzumaki",
                "Konoha, 142 apt.",
                4,
                "+7 800 355 35 35",
                5,
                "2020-06-06",
                "Saske, come back to Konoha",
                color.isEmpty() ? null : color
        );
    }

    @Step("Сериализация объекта заказа в JSON")
    private String serializeOrder(Order order) {
        Gson gson = new Gson();
        return gson.toJson(order);
    }

    @Step("Отправка запроса на создание заказа")
    private io.restassured.response.ValidatableResponse sendCreateOrderRequest(String jsonOrder) {
        return given()
                .header("Content-type", "application/json")
                .body(jsonOrder)
                .when()
                .post("/api/v1/orders")
                .then();
    }
}
