package io.apishift.resource;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestIdFilterTest {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @AfterEach
    void clearMdc() {
        MDC.remove("requestId");
    }

    @Test
    void generatesRequestIdWhenHeaderMissing() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        when(request.getHeaderString(REQUEST_ID_HEADER)).thenReturn(null);
        when(request.getHeaders()).thenReturn(headers);

        filter.filter(request);

        String requestId = headers.getFirst(REQUEST_ID_HEADER);
        assertNotNull(requestId);
        assertFalse(requestId.isBlank());
        assertEquals(requestId, MDC.get("requestId"));
    }

    @Test
    void preservesIncomingRequestId() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        when(request.getHeaderString(REQUEST_ID_HEADER)).thenReturn("abc12345");
        when(request.getHeaders()).thenReturn(headers);

        filter.filter(request);

        assertEquals("abc12345", headers.getFirst(REQUEST_ID_HEADER));
        assertEquals("abc12345", MDC.get("requestId"));
    }

    @Test
    void responseFilterEchoesRequestIdAndClearsMdc() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
        when(response.getHeaders()).thenReturn(responseHeaders);

        MDC.put("requestId", "trace-99");
        filter.filter(request, response);

        assertEquals("trace-99", responseHeaders.getFirst(REQUEST_ID_HEADER));
        assertNull(MDC.get("requestId"));
    }
}
