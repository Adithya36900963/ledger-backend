package snovasys.ledger.users.controllers;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.users.models.MpinRequest;
import snovasys.ledger.users.service.MpinService;

@RestController
@RequestMapping("/api/users/auth/mpin")
@RequiredArgsConstructor
public class MpinController {
    private final MpinService mpinService;

    @GetMapping("/status")
    public Map<String, Boolean> status(Authentication authentication) {
        return Map.of("configured", mpinService.isConfigured(authentication));
    }

    @PutMapping
    public Map<String, String> setup(Authentication authentication, @Valid @RequestBody MpinRequest request) {
        mpinService.setup(authentication, request.getMpin());
        return Map.of("message", "MPIN configured successfully");
    }
}
