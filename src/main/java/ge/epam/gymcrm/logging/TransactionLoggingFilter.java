package ge.epam.gymcrm.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionLoggingFilter extends OncePerRequestFilter {

    private static final Logger transactionLog = LoggerFactory.getLogger("TRANSACTION");
    private static final Logger restLog = LoggerFactory.getLogger("REST");

    private static final int MAX_PAYLOAD_LENGTH = 2_000;
    private static final Pattern SECRET = Pattern.compile(
            "(\"(?:\\w*[Pp]assword)\"\\s*:\\s*\")[^\"]*(\")");
    private static final Pattern QUERY_SECRET = Pattern.compile("([Pp]assword=)[^&]*");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String transactionId = resolveTransactionId(request);
        MDC.put(TransactionContext.TRANSACTION_ID, transactionId);
        response.setHeader(TransactionContext.TRANSACTION_ID_HEADER, transactionId);

        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        String endpoint = request.getMethod() + " " + requestUri(request);
        long startedAt = System.currentTimeMillis();
        transactionLog.info("Transaction started: {}", endpoint);

        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long tookMs = System.currentTimeMillis() - startedAt;
            restLog.info("Endpoint: {} | request: {} | status: {} | response: {}",
                    endpoint,
                    payload(cachedRequest.getContentAsByteArray()),
                    cachedResponse.getStatus(),
                    payload(cachedResponse.getContentAsByteArray()));
            transactionLog.info("Transaction finished: {} with status {} in {} ms",
                    endpoint, cachedResponse.getStatus(), tookMs);

            cachedResponse.copyBodyToResponse();
            MDC.remove(TransactionContext.TRANSACTION_ID);
        }
    }

    private String resolveTransactionId(HttpServletRequest request) {
        String incoming = request.getHeader(TransactionContext.TRANSACTION_ID_HEADER);
        return (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
    }

    private String requestUri(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + QUERY_SECRET.matcher(query).replaceAll("$1****");
    }

    private String payload(byte[] content) {
        if (content == null || content.length == 0) {
            return "<empty>";
        }
        String body = new String(content, StandardCharsets.UTF_8);
        if (body.length() > MAX_PAYLOAD_LENGTH) {
            body = body.substring(0, MAX_PAYLOAD_LENGTH) + "...<truncated>";
        }
        return SECRET.matcher(body).replaceAll("$1****$2");
    }
}
