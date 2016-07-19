package io.opentracing.contrib.dropwizard;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMapWriter;
import io.opentracing.contrib.dropwizard.ClientAttribute;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class ClientRequestTracingFilter implements ClientRequestFilter {

    private Request request;
    private DropWizardTracer tracer;
    private Set<ClientAttribute> tracedAttributes;
    private Set<String> tracedProperties;

    public ClientRequestTracingFilter(
        DropWizardTracer tracer, 
        Request request, 
        Set<ClientAttribute> tracedAttributes, 
        Set<String> tracedProperties
    ) {
        this.tracer = tracer;
        this.request = request;
        this.tracedAttributes = tracedAttributes;
        this.tracedProperties = tracedProperties;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        
        // set the operation name
        String operationName = requestContext.getUri().toString();

        // create the new span
        Span span = null;
        if(this.request != null) {
            Span parentSpan = this.tracer.getSpan(request);
            if(parentSpan == null) {
                span = this.tracer.getTracer().buildSpan(operationName).start();
            } else {
                span = this.tracer.getTracer().buildSpan(operationName).asChildOf(parentSpan.context()).start();
            }
        } else {
            span = this.tracer.getTracer().buildSpan(operationName).start();
        }

        // trace attributes

       for(ClientAttribute attribute : this.tracedAttributes) {
            switch(attribute) {
                case ACCEPTABLE_LANGUAGES: 
                    try { span.setTag("Acceptable Languages", requestContext.getAcceptableLanguages().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case ACCEPTABLE_MEDIA_TYPES: 
                    try{ span.setTag("Acceptable Media Types", requestContext.getAcceptableMediaTypes().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case COOKIES:
                    try { span.setTag("Cookies", requestContext.getCookies().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case DATE:
                    try { span.setTag("Date", requestContext.getDate().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case ENTITY:
                    try { span.log("Entity", requestContext.getEntity()); }
                    catch (NullPointerException npe) {}
                    break;
                case ENTITY_ANNOTATIONS:
                    try { span.setTag("Entity Stream", requestContext.getEntityStream().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case ENTITY_CLASS:
                    try { span.setTag("Entity Class", requestContext.getEntityClass().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case ENTITY_STREAM:
                    try { span.log("Entity Stream", requestContext.getEntityStream()); }
                    catch (NullPointerException npe) {}
                    break;
                case HEADERS:
                    try { span.setTag("Headers", requestContext.getHeaders().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case LANGUAGE:
                    try { span.setTag("Language", requestContext.getLanguage().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case MEDIA_TYPE: 
                    try { span.setTag("Media Type", requestContext.getMediaType().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case METHOD:
                    try { span.setTag("Method", requestContext.getMethod()); }
                    catch (NullPointerException npe) {}
                    break;
                case PROPERTY_NAMES:
                    try { span.setTag("Property Names", requestContext.getPropertyNames().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case URI:
                    try { span.setTag("URI", requestContext.getUri().toString()); }
                    catch (NullPointerException npe) {}
                    break;
            }
        }

        // trace properties

        for(String propertyName : this.tracedProperties) {
            Object property = requestContext.getProperty(propertyName);
            if(property != null) {
                span.log(propertyName, property);
            }
        }

        // add the new span to the tracer
        tracer.addClientSpan(requestContext, span);

        // add the span to the headers
        final MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        tracer.getTracer().inject(span.context(), new TextMapWriter() {
            public void put(String key, String value) {
                headers.add(key, value);
            }
        });
    }
}