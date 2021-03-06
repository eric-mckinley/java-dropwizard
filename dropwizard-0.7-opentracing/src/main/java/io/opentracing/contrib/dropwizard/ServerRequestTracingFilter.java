package io.opentracing.contrib.dropwizard;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * When registered to a DropWizard service along with a ServerResponseTracingFilter,
 * this filter creates a span for any requests to the service.
 */
public class ServerRequestTracingFilter implements ContainerRequestFilter {

    private final DropWizardTracer tracer;
    private final Set<ServerAttribute> tracedAttributes;
    private final Set<String> tracedProperties;
    private final String operationName;
    private final RequestSpanDecorator decorator;

    /**
     * @param tracer to trace requests with
     * @param operationName the operation name for the request spans
     * @param tracedAttributes any ServiceAttributes to log to spans
     * @param tracedProperties any request properties to log to spans
     * @param decorator an optional decorator for request spans
     */
    private ServerRequestTracingFilter(
        DropWizardTracer tracer,
        String operationName,
        Set<ServerAttribute> tracedAttributes, 
        Set<String> tracedProperties,
        RequestSpanDecorator decorator
    ) {
        this.tracer = tracer;
        this.operationName = operationName;
        this.tracedProperties = tracedProperties;
        this.tracedAttributes = tracedAttributes;
        this.decorator = decorator;
    }

    public static class Builder {

        private final DropWizardTracer tracer;
        private Set<ServerAttribute> tracedAttributes = new HashSet<ServerAttribute>();
        private Set<String> tracedProperties = new HashSet<String>();
        private String operationName = "";
        private RequestSpanDecorator decorator;

        /**
         * @param tracer the tracer to trace the server requests with
         */
        public Builder(DropWizardTracer tracer) {
            this.tracer = tracer;
        }

        /**
         * @param attributes a set of request attributes that you want 
         *  to tag to spans created for server requests
         * @return Builder configured with added traced attributes
         */
        public Builder withTracedAttributes(Set<ServerAttribute> attributes) {
            this.tracedAttributes = attributes;
            return this;
        }

        /**
         * @param properties of a request to the server to tag 
         *  to spans created for that request
         * @return Builder configured with added traced properties
         */
        public Builder withTracedProperties(Set<String> properties) {
            this.tracedProperties = properties;
            return this;
        }

        /**
         * @param decorator an (optional) RequestSpanDecorator which is applied to each [Request, Span] pair.
         * @return Builder for chaining
         */
        public Builder withRequestSpanDecorator(RequestSpanDecorator decorator) {
            this.decorator = decorator;
            return this;
        }

        /**
         * @param operationName for spans created by this filter
         * @return Builder configured with added operationName
         */
        public Builder withOperationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        /**
         * @return ServerRequestTracingFilter with the configuration of this Builder 
         */
        public ServerRequestTracingFilter build() {
            return new ServerRequestTracingFilter(this.tracer, this.operationName,
                this.tracedAttributes, this.tracedProperties, this.decorator);
        }
    }
    
    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String operationName;
        if (this.operationName == "") {
            operationName = request.getRequestUri().toString();
        } else {
            operationName = this.operationName;
        }
        // format the headers for extraction
        Span span;
        MultivaluedMap<String, String> rawHeaders = request.getRequestHeaders();
        final HashMap<String, String> headers = new HashMap<String, String>();
        for (String key : rawHeaders.keySet()){
            headers.put(key, rawHeaders.get(key).get(0));
        }

        // extract the client span
        try {
            SpanContext parentSpan = tracer.getTracer().extract(
                    Format.Builtin.HTTP_HEADERS,
                    new TextMapExtractAdapter(headers));
            if (parentSpan == null){
                span = tracer.getTracer().buildSpan(operationName).start();
            } else {
                span = tracer.getTracer().buildSpan(operationName).asChildOf(parentSpan).start();
            }
        } catch(IllegalArgumentException e) {
            span = tracer.getTracer().buildSpan(operationName).start();
        }

        // trace attributes
        for (ServerAttribute attribute : this.tracedAttributes) {
            switch(attribute) {
                case ABSOLUTE_PATH: 
                    try { span.setTag("Absolute Path", request.getAbsolutePath().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case ACCEPTABLE_LANGUAGES: 
                    try { span.setTag("Acceptable Languages", request.getAcceptableLanguages().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case ACCEPTABLE_MEDIA_TYPES: 
                    try { span.setTag("Acceptable Media Types", request.getAcceptableMediaTypes().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                 case AUTHENTICATION_SCHEME: 
                    try { span.setTag("Authentication Scheme", request.getAuthenticationScheme()); }
                    catch(NullPointerException npe) {}
                    break;
                case BASE_URI: 
                    try { span.setTag("Base URI", request.getBaseUri().toString()); }
                    catch(NullPointerException npe) {}
                    break;
               case COOKIES:
                    try { span.setTag("Cookies", request.getCookies().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case HEADERS:
                    try { span.setTag("Headers", request.getRequestHeaders().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case IS_SECURE: 
                    try { span.setTag("Is Secure", request.isSecure()); }
                    catch(NullPointerException npe) {}
                    break;
                case LANGUAGE:
                    try { span.setTag("Language", request.getLanguage().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case METHOD:
                    try { span.setTag("Method", request.getMethod()); }
                    catch(NullPointerException npe) {}
                    break;
                case MEDIA_TYPE: 
                    try { span.setTag("Media Type", request.getMediaType().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case PATH:
                    try { span.setTag("Property Names", request.getPath()); }
                    catch(NullPointerException npe) {}
                    break;
                 case QUERY_PARAMETERS: 
                    try { span.setTag("Query Paramters", request.getQueryParameters().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case SECURITY_CONTEXT:
                    try { span.setTag("Security Context", request.getSecurityContext().getAuthenticationScheme()); }
                    catch(NullPointerException npe) {}
                    break;
                case URI:
                    try { span.setTag("URI", request.getRequestUri().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case USER_PRINCIPAL: 
                    try { span.setTag("User Principal", request.getUserPrincipal().getName()); }
                    catch(NullPointerException npe) {}
                    break;
            }
        }

        // trace properties
        for (String propertyName : this.tracedProperties) {
            Object property = request.getProperties().get(propertyName);
            if (property != null) {
                span.log(propertyName, property);
            }
        }

        if (this.decorator != null) {
            this.decorator.decorate(request, span);
        }

        // add the new span to the trace
        tracer.addServerSpan(request, span);
        return request;
    }
}
