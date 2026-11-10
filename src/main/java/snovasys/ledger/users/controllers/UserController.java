package snovasys.ledger.users.controllers;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.LoginModel;
import snovasys.ledger.users.models.RegisterModel;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.service.Auth.UserAuthService;
import snovasys.ledger.audit.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAuthService userAuthService;
    private final AuditLogService auditLog;

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/auth/register")
    public Map<Object, Object> register(
            Authentication authentication, @Valid @RequestBody RegisterModel registerModel) {

        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can create users");
        }
        if (registerModel.getRole() != Role.ACCOUNT_HOLDER && registerModel.getRole() != Role.AUDITOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins can create account holders or auditors");
        }
        Map<Object, Object> result = userAuthService.register(registerModel);
        if (Boolean.TRUE.equals(result.get("success"))) {
            auditLog.record(authentication, "USER_CREATED", "SUCCESS", "userId=" + result.get("userId")
                    + ", role=" + result.get("role"));
        }
        return result;
    }

    @GetMapping("/auth/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return Map.of("userId", user.getId(), "name", user.getUsername(), "email", user.getEmail(),
                "role", user.getRole().name(), "isEmailVerified", Boolean.TRUE.equals(user.getEmailVerified()),
                "isNumberVerified", Boolean.TRUE.equals(user.getMobileVerified()));
    }

    @PostMapping("/auth/auditor/login")
    public Map<Object, Object> loginAuditor(
            @RequestBody LoginModel loginModel,
            HttpServletRequest request,
            HttpServletResponse response) {
        return userAuthService.login(loginModel, response, request.getRemoteAddr(), "AUDITOR");
    }

    // =========================
    // LOGIN
    // =========================

    // =========================
    // LOGIN WITH RATE LIMITING
    // =========================

    @PostMapping("/auth/ACCOUNT_HOLDER/login")
    public Map<Object, Object> loginACCOUNT_HOLDER(
            @RequestBody LoginModel loginModel,
            HttpServletRequest request,
            HttpServletResponse response) {

        String clientIp = request.getRemoteAddr();

        return userAuthService.loginACCOUNT_HOLDER(
                loginModel,
                response,
                clientIp
        );
    }
    @PostMapping("/auth/admin/login")
    public Map<Object, Object> loginAdmin(
            @RequestBody LoginModel loginModel,
            HttpServletRequest request,
            HttpServletResponse response) {

        String clientIp = request.getRemoteAddr();

        return userAuthService.loginAdmin(
                loginModel,
                response,
                clientIp
        );
    }
    // =========================
    // LOGOUT
    // =========================

    @PostMapping("/auth/logout")
    public Map<Object, Object> logout(HttpServletResponse response) {
        return userAuthService.logout(response);
    }
    // =========================
    // EMAIL VERIFICATION
    // =========================

    @GetMapping("/auth/email/{token}")
    public Map<Object, Object> verifyEmail(
            @PathVariable String token) {

        return userAuthService.verifyEmail(token);
    }

    @PostMapping("/auth/email")
    public Map<Object, Object> generateEmailVerificationLink(
            Authentication authentication,HttpServletRequest httpServletRequest) {

        User user = (User) authentication.getPrincipal();

        return userAuthService
                .generateEmailVerificationLink(user,httpServletRequest);
    }

    // =========================
    // MOBILE VERIFICATION
    // =========================

    @PostMapping("/auth/mobile")
    public Map<Object, Object> generateMobileVerificationToken(
            Authentication authentication,HttpServletRequest httpServletRequest) {

        User user = (User) authentication.getPrincipal();

        return userAuthService
                .generateMobileVerificationToken(user,httpServletRequest);
    }

    @GetMapping("/auth/mobile/{token}")
    public Map<Object, Object> verifyMobile(
            @PathVariable String token) {

        return userAuthService.verifyMobile(token);
    }
}
