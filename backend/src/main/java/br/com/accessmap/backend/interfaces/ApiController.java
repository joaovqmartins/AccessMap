package br.com.accessmap.backend.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "accessmap-api"));
    }

    @GetMapping("/api")
    public ResponseEntity<Map<String, String>> api() {
        return ResponseEntity.ok(Map.of("message", "AccessMap API está funcionando"));
    }
}
