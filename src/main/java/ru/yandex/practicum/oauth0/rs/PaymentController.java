package ru.yandex.practicum.oauth0.rs;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_payments:read')")
    public ResponseEntity<Map<String, Object>> getPayments() {
        List<String> payments = List.of("Payment_100$", "Payment_250$");
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", payments
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_payments:write')")
    public ResponseEntity<Map<String, Object>> createPayment() {
        return ResponseEntity.ok(Map.of(
                "status", "created",
                "message", "Payment successfully processed"
        ));
    }
}