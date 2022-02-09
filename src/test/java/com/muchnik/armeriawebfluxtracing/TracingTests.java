package com.muchnik.armeriawebfluxtracing;

import com.linecorp.armeria.common.thrift.ThriftSerializationFormats;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.thrift.THttpService;
import com.muchnik.armeriawebfluxtracing.iface.MultiplicationService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@AutoConfigureWebTestClient
@SpringBootTest
public class TracingTests {
    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void beforeAll() {
        Server.builder()
              .service("/multiply", THttpService.of((MultiplicationService.AsyncIface) (n1, n2, resultHandler) -> {
                  int result = n1 * n2;
                  resultHandler.onComplete(result);
              }, ThriftSerializationFormats.BINARY))
              .http(9090)
              .build()
              .start();
    }

    @Test
    @SneakyThrows
    void test() {
        webTestClient.get().uri("/test")
                     .exchange()
                     .expectStatus()
                     .is2xxSuccessful()
                     .expectBody()
                     .jsonPath("$.result").isEqualTo(20);
    }

}
