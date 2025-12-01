package com.bajaj.test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;

@SpringBootApplication
public class HiringApp implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(HiringApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        // Step 1: Send POST request to generate webhook
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        String requestJson = """
            {
              "name": "beedam prasanth sai",
              "regNo": "22BCE1767",
              "email": "prasanthbeedam@gmail.com"
            }
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        System.out.println("Response: " + response.getBody());

        String webhookUrl = (String) response.getBody().get("webhook");
        String accessToken = (String) response.getBody().get("accessToken");
        String regNo = "REG12347";
        int lastTwo = Integer.parseInt(regNo.substring(regNo.length() - 2));
        String finalQuery;

        if (lastTwo % 2 == 1) {
            finalQuery = "SELECT \n" +
                    "    d.DEPARTMENT_NAME,\n" +
                    "    t.SALARY,\n" +
                    "    CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME,\n" +
                    "    FLOOR(DATEDIFF(CURDATE(), e.DOB) / 365) AS AGE\n" +
                    "FROM (\n" +
                    "    SELECT \n" +
                    "        e.EMP_ID,\n" +
                    "        e.DEPARTMENT,\n" +
                    "        SUM(p.AMOUNT) AS SALARY\n" +
                    "    FROM EMPLOYEE e\n" +
                    "    JOIN PAYMENTS p \n" +
                    "        ON e.EMP_ID = p.EMP_ID\n" +
                    "    WHERE DAY(p.PAYMENT_TIME) != 1\n" +
                    "    GROUP BY e.EMP_ID, e.DEPARTMENT\n" +
                    ") t\n" +
                    "JOIN (\n" +
                    "    SELECT \n" +
                    "        DEPARTMENT,\n" +
                    "        MAX(SALARY) AS MAX_SAL\n" +
                    "    FROM (\n" +
                    "        SELECT \n" +
                    "            e.EMP_ID,\n" +
                    "            e.DEPARTMENT,\n" +
                    "            SUM(p.AMOUNT) AS SALARY\n" +
                    "        FROM EMPLOYEE e\n" +
                    "        JOIN PAYMENTS p \n" +
                    "            ON e.EMP_ID = p.EMP_ID\n" +
                    "        WHERE DAY(p.PAYMENT_TIME) != 1\n" +
                    "        GROUP BY e.EMP_ID, e.DEPARTMENT\n" +
                    "    ) x\n" +
                    "    GROUP BY DEPARTMENT\n" +
                    ") m \n" +
                    "    ON t.DEPARTMENT = m.DEPARTMENT \n" +
                    "   AND t.SALARY = m.MAX_SAL\n" +
                    "JOIN EMPLOYEE e ON e.EMP_ID = t.EMP_ID\n" +
                    "JOIN DEPARTMENT d ON d.DEPARTMENT_ID = t.DEPARTMENT\n" +
                    "ORDER BY d.DEPARTMENT_NAME;";
        } else {
            finalQuery = "SELECT \n" +
                    "    e1.EMP_ID,\n" +
                    "    e1.FIRST_NAME,\n" +
                    "    e1.LAST_NAME,\n" +
                    "    d.DEPARTMENT_NAME,\n" +
                    "    COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT\n" +
                    "FROM EMPLOYEE e1\n" +
                    "JOIN DEPARTMENT d \n" +
                    "    ON e1.DEPARTMENT = d.DEPARTMENT_ID\n" +
                    "LEFT JOIN EMPLOYEE e2 \n" +
                    "    ON e1.DEPARTMENT = e2.DEPARTMENT\n" +
                    "   AND e2.DOB > e1.DOB   -- e2 is younger than e1\n" +
                    "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME\n" +
                    "ORDER BY e1.EMP_ID DESC;\n";
        }

        String submitUrl = webhookUrl;

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        Map<String, String> requestBody = Map.of("finalQuery", finalQuery);
        HttpEntity<Map<String, String>> entitySubmit = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> submitResponse =
                restTemplate.postForEntity(submitUrl, entitySubmit, String.class);

        System.out.println("Submit Response: " + submitResponse.getBody());

    }
}
