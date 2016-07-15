package io.opentracing.dropwizard;

import io.opentracing.Span;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

public class ClientResponseTracingFilter implements ClientResponseFilter {

    private DropWizardTracer tracer;

    public ClientResponseTracingFilter(DropWizardTracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        this.tracer.finishClientSpan(requestContext);
    }
}