package com.muchnik.armeriawebfluxtracing.client;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext;
import com.linecorp.armeria.client.Clients;
import com.linecorp.armeria.client.brave.BraveClient;
import com.linecorp.armeria.common.brave.RequestContextCurrentTraceContext;
import com.linecorp.armeria.common.logging.RequestLogProperty;
import com.muchnik.armeriawebfluxtracing.iface.MultiplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ArmeriaClientConfiguration {

    // private static final CurrentTraceContext traceCtx =
    //         RequestContextCurrentTraceContext.builder()
    //                                          .addScopeDecorator(MDCScopeDecorator.get())
    //                                          .build();
    // private static final Tracing tracing = Tracing.newBuilder()
    //                                               .currentTraceContext(traceCtx)
    //                                               .build();

    @Bean
    public MultiplicationService.AsyncIface multiplicationServiceClient() {
        return Clients.builder("tbinary+http://localhost:9090/multiply")
                      //  BraveClient decorator not working properly with BraveAutoConfiguration#tracing()
                      //  Which is based on ThreadLocalCurrentTraceContext
                      //  .decorator(BraveClient.newDecorator(tracing))
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
