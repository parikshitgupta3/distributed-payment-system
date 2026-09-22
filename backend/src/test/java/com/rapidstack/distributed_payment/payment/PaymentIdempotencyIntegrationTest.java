package com.rapidstack.distributed_payment.payment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.rapidstack.distributed_payment.payment.entity.Payment;
import com.rapidstack.distributed_payment.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentIdempotencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    PaymentRepository paymentRepository;

    @Value("${local.server.port}")
    int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sameIdempotencyKeyTwiceCreatesOnlyOnePayment() throws Exception {
        String key = UUID.randomUUID().toString();

        long firstId = postPayment(key);
        long secondId = postPayment(key);

        assertThat(secondId).isEqualTo(firstId);
        assertThat(paymentsWithKey(key)).hasSize(1);
    }

    @Test
    void concurrentDuplicateRequestsCreateOnlyOnePayment() throws Exception {
        String key = UUID.randomUUID().toString();
        int threads = 8;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier startGate = new CyclicBarrier(threads);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await(10, TimeUnit.SECONDS);
                    return postPayment(key);
                }));
            }

            Set<Long> ids = futures.stream()
                    .map(this::uncheckedGet)
                    .collect(Collectors.toSet());

            assertThat(ids).hasSize(1);
            assertThat(paymentsWithKey(key)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void missingIdempotencyKeyIsRejected() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/payments"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"userId\":1,\"amount\":10.00,\"currency\":\"INR\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
    }

    private long postPayment(String idempotencyKey) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/payments"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString("{\"userId\":1,\"amount\":25.50,\"currency\":\"INR\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode body = mapper.readTree(response.body());
        return body.get("id").asLong();
    }

    private List<Payment> paymentsWithKey(String key) {
        return paymentRepository.findAll().stream()
                .filter(p -> key.equals(p.getIdempotencyKey()))
                .toList();
    }

    private long uncheckedGet(Future<Long> future) {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
