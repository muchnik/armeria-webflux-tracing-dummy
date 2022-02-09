package com.muchnik.armeriawebfluxtracing.controller;

import com.linecorp.armeria.common.thrift.ThriftFuture;
import com.muchnik.armeriawebfluxtracing.iface.MultiplicationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Contr {
    private final MultiplicationService.AsyncIface client;

    private final WebClient webClient = WebClient.builder()
                                                 .baseUrl("http://google.com")
                                                 .filter((request, next) -> {
                                                     log.info("WebClient call"); // to see traceId in logs
                                                     return next.exchange(request);
                                                 })
                                                 .build();


    @SneakyThrows
    @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Response> test() {
        return webClient.get()
                        .exchangeToMono(Mono::just)
                        .flatMap(unused -> callThrift())
                        .flatMap(unused -> webClient.get().exchangeToMono(Mono::just))
                        .flatMap(unused -> callThrift());
    }

    private Mono<Response> callThrift() {
        ThriftFuture<Integer> future = new ThriftFuture<>();
        try {
            client.multiply(4, 5, future);
        } catch (TException e) {
            log.error("Exception {}", e.getMessage(), e);
        }
        return Mono.fromFuture(future)
                   .map(Response::new)
                   .switchIfEmpty(Mono.error(new IllegalStateException("Empty future!")))
                   .doOnError(throwable -> log.error("Exception: {}", throwable.getMessage(), throwable));
    }

    @Data
    @AllArgsConstructor
    static final class Response {
        private Integer result;
    }
}
