package ru.yandex.scooter;

import com.google.gson.Gson;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.given;

@RunWith(Parameterized.class)
public class OrderListTests extends BaseTest {

    private int courierId;

    // Параметры для параметризации
    private final String testDescription;
    private final Map<String, Object> queryParams;
    private final int expectedStatusCode;
    private final String expectedMessage;
    private final boolean expectOrdersNotEmpty;

    public OrderListTests(String testDescription, Map<String, Object> queryParams, int expectedStatusCode, String expectedMessage, boolean expectOrdersNotEmpty) {
        this.testDescription = testDescription;
        this.queryParams = queryParams;
        this.expectedStatusCode = expectedStatusCode;
        this.expectedMessage = expectedMessage;
        this.expectOrdersNotEmpty = expectOrdersNotEmpty;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getTestData() {
        List<Object[]> testData = new ArrayList<>();

        testData.add(new Object[]{
                "Получение списка заказов без параметров",
                null,
                200,
                null,
                true
        });

        testData.add(new Object[]{
                "Получение заказов с существующим courierId",
                new HashMap<String, Object>() {{
                    put("courierId", "VALID_COURIER_ID");
                }},
                200,
                null,
                true
        });

        testData.add(new Object[]{
                "Получение заказов с несуществующим courierId",
                new HashMap<String, Object>() {{
                    put("courierId", 9999999);
                }},
                404,
                "Курьер с идентификатором 9999999 не найден",
                false
        });

        testData.add(new Object[]{
                "Получение списка заказов с параметрами limit и page",
                new HashMap<String, Object>() {{
                    put("limit", 10);
                    put("page", 0);
                }},
                200,
                null,
                true
        });

        testData.add(new Object[]{
                "Получение заказов, отфильтрованных по ближайшей станции",
                new HashMap<String, Object>() {{
                    String[] stations = {"1", "2"};
                    String stationsJson = new Gson().toJson(stations);
                    put("nearestStation", stationsJson);
                }},
                200,
                null,
                true
        });

        testData.add(new Object[]{
                "Получение заказов с ограничением и фильтром по станции",
                new HashMap<String, Object>() {{
                    put("limit", 10);
                    put("page", 0);
                    String[] stations = {"110"};
                    String stationsJson = new Gson().toJson(stations);
                    put("nearestStation", stationsJson);
                }},
                200,
                null,
                true
        });

        return testData;
    }

    @Before
    public void setUp() {
        generateCourierData();
        courierId = createCourier(courierLogin, courierPassword, courierFirstName);

        // Если тест требует наличия заказа у курьера, создаем и назначаем заказ
        if ("Получение заказов с существующим courierId".equals(testDescription)) {
            int track = createOrder(getDefaultOrderData());
            int orderId = getOrderIdByTrack(track);
            acceptOrder(orderId, courierId);
        }

        // Заменяем плейсхолдер на реальный courierId
        if (queryParams != null && queryParams.containsValue("VALID_COURIER_ID")) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                if ("VALID_COURIER_ID".equals(entry.getValue())) {
                    entry.setValue(courierId);
                }
            }
        }
    }

    @Step("Получение стандартных данных для заказа")
    private Map<String, Object> getDefaultOrderData() {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("firstName", "Naruto");
        orderData.put("lastName", "Uzumaki");
        orderData.put("address", "Konoha, 142 apt.");
        orderData.put("metroStation", 4);
        orderData.put("phone", "+7 800 355 35 35");
        orderData.put("rentTime", 5);
        orderData.put("deliveryDate", "2020-06-06");
        orderData.put("comment", "Saske, come back to Konoha");
        orderData.put("color", Arrays.asList("BLACK"));
        return orderData;
    }

    @Test
    @DisplayName("Тест получения списка заказов с различными параметрами")
    @Description("{0}")
    public void testGetOrderListWithVariousParameters() {
        sendGetOrdersRequestAndCheckResponse(queryParams, expectedStatusCode, expectedMessage, expectOrdersNotEmpty);
    }

    @Step("Отправка запроса на получение заказов и проверка ответа")
    private void sendGetOrdersRequestAndCheckResponse(Map<String, Object> queryParams, int expectedStatusCode, String expectedMessage, boolean expectOrdersNotEmpty) {
        RequestSpecification requestSpec = given();

        if (queryParams != null) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                requestSpec.queryParam(entry.getKey(), entry.getValue());
            }
        }

        ValidatableResponse response = requestSpec
                .when()
                .get("/api/v1/orders")
                .then()
                .statusCode(expectedStatusCode);

        if (expectedStatusCode == 200) {
            response.body("orders", notNullValue());
            if (expectOrdersNotEmpty) {
                response.body("orders", not(empty()));
            }
        } else if (expectedMessage != null) {
            response.body("message", equalTo(expectedMessage));
        }
    }

    @After
    public void tearDown() {
        if (courierId != 0) {
            deleteCourier(courierId);
        }
    }
}
