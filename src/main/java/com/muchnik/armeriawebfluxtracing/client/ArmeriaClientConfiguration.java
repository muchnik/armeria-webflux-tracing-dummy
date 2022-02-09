package com.muchnik.armeriawebfluxtracing.client;

import brave.Tracing;
import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.common.logging.RequestLogProperty;
import com.muchnik.armeriawebfluxtracing.iface.MultiplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ArmeriaClientConfiguration {

    @Bean
    public MultiplicationService.AsyncIface multiplicationServiceClient() {
        return Clients.builder("tbinary+http://localhost:9090/multiply")
                      .decorator((delegate, ctx, req) -> {
                          String traceId = Tracing.current().currentTraceContext().get().traceIdString();
                          ctx.log()
                             .whenAvailable(RequestLogProperty.RESPONSE_HEADERS)
                             .thenAcceptAsync(requestLog -> {
                                 log.info("TraceId in thrift callback: {}", traceId);
                             }, ctx.eventLoop());

                          return delegate.execute(ctx, req);
                      })
                      .build(MultiplicationService.AsyncIface.class);
    }
}
