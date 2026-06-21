package ru.yandex.practicum.oauth0.auth.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.oauth0.auth.dto.ErrorResponse;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

@RestControllerAdvice
public class CommonErrorHandler {

    private static final Logger fileLogger = LoggerFactory.getLogger("ERROR_FILE_LOGGER");

    private static final List<String> SENSITIVE_KEYS = Arrays.asList("password", "client_secret", "clientsecret", "authorization");

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();

        String error;
        if (status.equals(HttpStatus.UNAUTHORIZED)) {
            error = "invalid_client";
        } else if (status.equals(HttpStatus.FORBIDDEN)) {
            error = "access_denied";
        } else if (status.equals(HttpStatus.BAD_REQUEST)) {
            error = "invalid_request";
        } else {
            error = "server_error";
        }

        return buildErrorResponse(HttpStatus.valueOf(status.value()), error, ex.getReason());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "invalid_request", "Invalid JSON or parameters");
    }

    @ExceptionHandler(UnsupportedGrantTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedGrantType(UnsupportedGrantTypeException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "unsupported_grant_type", ex.getMessage());
    }

    @ExceptionHandler(InvalidClientException.class)
    public ResponseEntity<ErrorResponse> handleInvalidClient(InvalidClientException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "invalid_client", ex.getMessage());
    }

    @ExceptionHandler(InvalidGrantException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGrant(InvalidGrantException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "invalid_grant", ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "invalid_token", ex.getMessage());
    }

    @ExceptionHandler(InsufficientScopeException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientScope(InsufficientScopeException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "insufficient_scope", ex.getMessage());
    }

    @ExceptionHandler(UserBlockedException.class)
    public ResponseEntity<ErrorResponse> handleUserBlocked(UserBlockedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "access_denied", ex.getMessage());
    }


    @ExceptionHandler(TokenReuseException.class)
    public ResponseEntity<ErrorResponse> handleTokenReuse(TokenReuseException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "invalid_grant", ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "temporarily_unavailable", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        logSecureContext(ex, request);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "server_error", "An internal server error occurred");
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String error, String description) {
        ErrorResponse response = ErrorResponse.builder()
                .error(error)
                .errorDescription(description)
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private void logSecureContext(Exception ex, HttpServletRequest request) {
        StringBuilder context = new StringBuilder();
        context.append("Method: ").append(request.getMethod()).append(" | ");
        context.append("URI: ").append(request.getRequestURI()).append(" | ");

        context.append("Params: [");
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = maskIfSensitive(paramName, request.getParameter(paramName));
            context.append(paramName).append("=").append(paramValue).append(", ");
        }
        context.append("] | ");

        context.append("Client IP: ").append(request.getRemoteAddr());

        fileLogger.error("Internal Server Error context: {} | Exception: {}", context, ex.getMessage(), ex);
    }

    private String maskIfSensitive(String key, String value) {
        if (value == null) return "null";

        String lowerKey = key.toLowerCase();
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return "******";
            }
        }
        return value;
    }
}