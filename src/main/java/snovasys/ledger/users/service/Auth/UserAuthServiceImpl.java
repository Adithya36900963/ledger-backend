package snovasys.ledger.users.service.Auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import snovasys.ledger.security.common.email.EmailService;
import snovasys.ledger.security.common.whathsapp.WhathsappService;
import snovasys.ledger.security.entity.VerificationToken;
import snovasys.ledger.security.jwt.JWTUtil;
import snovasys.ledger.security.rateLimiting.LoginAttemptLimiter;
import snovasys.ledger.security.services.EmailVerificationTokenService.EmailAndNumberVerificationTokenService;
import snovasys.ledger.users.entity.ACCOUNT_HOLDER;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.LoginModel;
import snovasys.ledger.users.models.RegisterModel;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.models.WhatsAppRequest;
import snovasys.ledger.users.repository.ACCOUNT_HOLDERRepo;
import snovasys.ledger.users.repository.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.security.rateLimiting.LoginAttemptLimiter;
import snovasys.ledger.security.rateLimiting.LoginRateLimitException;
import snovasys.ledger.audit.service.AuditLogService;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserRepo userRepo;
    private final ACCOUNT_HOLDERRepo ACCOUNT_HOLDERRepo;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final EmailService emailService;
    private final WhathsappService whathsappService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final AuditLogService auditLog;

    private final EmailAndNumberVerificationTokenService
            emailVerificationTokenService;

    
    // =========================================================
    // REGISTER
    // =========================================================

    @Override
    @Transactional
    public Map<Object, Object> register(
            RegisterModel registerModel) {

        if (registerModel.getRole() == null) {
            return Map.of(
                    "success", false,
                    "message", "Role is required"
            );
        }

        User user = userRepo.save(
                User.builder()
                        .username(registerModel.getUserName())
                        .email(registerModel.getEmail())
                        .number(registerModel.getNumber())
                        .password(
                                passwordEncoder.encode(
                                        registerModel.getPassword()
                                )
                        )
                        .role(registerModel.getRole())
                        .emailVerified(false)
                        .mobileVerified(false)
                        .build()
        );

        Map<Object, Object> res = new HashMap<>();

        Long holderId = null;
        if (user.getRole() == Role.ACCOUNT_HOLDER) {
            ACCOUNT_HOLDER holder = ACCOUNT_HOLDERRepo.save(
                    ACCOUNT_HOLDER.builder().user(user).build());
            holderId = holder.getId();
        } else if (user.getRole() != Role.AUDITOR) {
            throw new IllegalArgumentException("Only account holders and auditors can be created here");
        }

        res.put("success", true);
        res.put("message", "User created successfully");
        res.put("userId", user.getId());
        if (holderId != null) res.put("accountHolderId", holderId);
        res.put("role", user.getRole().name());
        res.put("isEmailVerified", false);
        res.put("isNumberVerified", false);
        return res;
    }

    // =========================================================
    // LOGIN
    // =========================================================

        @Override
        public Map<Object, Object> login(
                LoginModel loginModel,
                HttpServletResponse response,
                String clientIp,String role) {

        Map<Object, Object> res = new HashMap<>();

        String login = loginModel.getLogin().trim();

        // Resolve the account key so email and phone logins
        // for the same existing user share one account counter.
        User user = userRepo.findByEmailOrNumber(login);
        

        String accountKey = user != null
                ? user.getEmail()
                : login;

        // 1. Reject requests while account or IP is locked.
        long retryAfter =
                loginAttemptLimiter.blockedForSeconds(
                        accountKey, clientIp);

        if (retryAfter > 0) {
                throw new LoginRateLimitException(retryAfter);
        }

        // 2. Do not reveal whether an account exists.
        if (user == null) {
                retryAfter = loginAttemptLimiter.recordFailure(
                        accountKey, clientIp);

                if (retryAfter > 0) {
                throw new LoginRateLimitException(retryAfter);
                }

                res.put("success", false);
                res.put("message", "Invalid login credentials");
                return res;
        }
        if (!user.getRole().name().equals(role)) {
                retryAfter = loginAttemptLimiter.recordFailure(accountKey, clientIp);
                if (retryAfter > 0) throw new LoginRateLimitException(retryAfter);
                res.put("success", false);
                res.put("message", "Invalid login credentials");
                return res;
        }

        // 3. Verify password.
        boolean passwordMatches = passwordEncoder.matches(
                loginModel.getPassword(),
                user.getPassword());

        if (!passwordMatches) {
                retryAfter = loginAttemptLimiter.recordFailure(
                        accountKey, clientIp);

                if (retryAfter > 0) {
                throw new LoginRateLimitException(retryAfter);
                }

                res.put("success", false);
                res.put("message", "Invalid login credentials");
                return res;
        }

        // 4. Successful login: clear account failures.
        // Preserve IP failure history to prevent account hopping.
        loginAttemptLimiter.recordSuccess(accountKey);
        auditLog.recordUser(user, "LOGIN_SUCCESS", "SUCCESS", "role=" + user.getRole().name());

        // 5. Preserve your existing response structure.
        res.put("success", true);
        res.put("message", "Login successful");

        res.put("role", user.getRole());
        res.put("name", user.getUsername());
        res.put("email", user.getEmail());
        res.put("number", user.getNumber());

        res.put("isNumberVerified", user.getMobileVerified());
        res.put("isEmailVerified", user.getEmailVerified());

        // 6. Preserve your existing JWT and cookie logic.
        String jwt = jwtUtil.generateToken(user.getEmail());

        ResponseCookie cookie = ResponseCookie
                .from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite("None")
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        return res;
        }


        @Override
        public Map<Object, Object> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<Object, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Logout successful");

        return res;
        }


    // =========================================================
    // VERIFY EMAIL
    // =========================================================

    @Override
    public Map<Object, Object> verifyEmail(
            String token) {

        Map<Object, Object> res =
                new HashMap<>();

        User user =
                emailVerificationTokenService
                        .verifyEmailToken(token);

        user.setEmailVerified(true);

        userRepo.save(user);
        auditLog.recordUser(user, "EMAIL_VERIFIED", "SUCCESS", "User email verification completed");

        res.put("success", true);
        res.put(
                "message",
                "Email verified successfully"
        );

        res.put(
                "isEmailVerified",
                true
        );

        res.put(
                "isNumberVerified",
                user.getMobileVerified()
        );

        return res;
    }

    // =========================================================
    // GENERATE EMAIL VERIFICATION LINK
    // =========================================================

    @Override
    public Map<Object, Object> generateEmailVerificationLink(
            User user,HttpServletRequest httpServletRequest) {

        Map<Object, Object> res =
                new HashMap<>();

        if (Boolean.TRUE.equals(
                user.getEmailVerified())) {

            res.put("success", false);
            res.put(
                    "message",
                    "Email is already verified"
            );

            res.put(
                    "isEmailVerified",
                    true
            );

            res.put(
                    "isNumberVerified",
                    user.getMobileVerified()
            );

            return res;
        }

        VerificationToken verificationToken =
                emailVerificationTokenService
                        .createEmailToken(user);

        String token =
                verificationToken.getToken();

        /*
         * Create your actual email verification URL here.
         *
         * Example:
         *
         * String link =
         *     "http://localhost:8080/api/users/auth/email/"
         *     + token;
         *
         * Then send this link using your EmailService.
         */

        res.put("success", true);
        res.put(
                "message",
                "Email verification link created successfully"
        );

        // For development/testing only.
        // Don't return the token in production.

        
        res.put(
                "isEmailVerified",
                false
        );

        res.put(
                "isNumberVerified",
                user.getMobileVerified()
        );
         String baseUrl=getBaseUrl(httpServletRequest);
        emailService.sendEmail(user.getEmail(), "Email Verification Token", baseUrl+"/api/users/auth/email/"+token);
        return res;
    }

    // =========================================================
    // GENERATE MOBILE VERIFICATION TOKEN
    // =========================================================

    @Override
    public Map<Object, Object> generateMobileVerificationToken(
            User user,HttpServletRequest httpServletRequest) {

        Map<Object, Object> res =
                new HashMap<>();

        if (Boolean.TRUE.equals(
                user.getMobileVerified())) {

            res.put("success", false);
            res.put(
                    "message",
                    "Mobile number is already verified"
            );

            res.put(
                    "isNumberVerified",
                    true
            );

            res.put(
                    "isEmailVerified",
                    user.getEmailVerified()
            );

            return res;
        }

        VerificationToken verificationToken =
                emailVerificationTokenService
                        .createMobileToken(user);

        String token =
                verificationToken.getToken();

        /*
         * Send this token using your SMS service.
         *
         * Example:
         *
         * smsService.sendVerificationToken(
         *     user.getNumber(),
         *     token
         * );
         */

        res.put("success", true);
        res.put(
                "message",
                "Mobile verification link created successfully"
        );

        

        res.put(
                "isNumberVerified",
                false
        );

        res.put(
                "isEmailVerified",
                user.getEmailVerified()
        );
        String baseUrl=getBaseUrl(httpServletRequest);
        WhatsAppRequest request=new WhatsAppRequest(user.getNumber(),baseUrl+"/api/users/auth/mobile/"+token);
        whathsappService.send(request);
        return res;
    }

    // =========================================================
    // VERIFY MOBILE
    // =========================================================

    @Override
    public Map<Object, Object> verifyMobile(
            String token) {

        Map<Object, Object> res =
                new HashMap<>();

        User user =
                emailVerificationTokenService
                        .verifyMobileToken(token);

        user.setMobileVerified(true);

        userRepo.save(user);
        auditLog.recordUser(user, "MOBILE_VERIFIED", "SUCCESS", "User mobile verification completed");

        res.put("success", true);
        res.put(
                "message",
                "Mobile number verified successfully"
        );

        res.put(
                "isNumberVerified",
                true
        );

        res.put(
                "isEmailVerified",
                user.getEmailVerified()
        );

        return res;
    }

    @Override
    public String getBaseUrl(HttpServletRequest httpServletRequest) {
        String baseUrl = httpServletRequest.getScheme() + "://"
            + httpServletRequest.getServerName() + ":"
            + httpServletRequest.getServerPort();
        return  baseUrl;
    }

    @Override
    public Map<Object, Object> loginACCOUNT_HOLDER(LoginModel loginModel, HttpServletResponse response, String clientIp) {
        return login(loginModel, response, clientIp,"ACCOUNT_HOLDER");
    }

    @Override
    public Map<Object, Object> loginAdmin(LoginModel loginModel, HttpServletResponse response, String clientIp) {
        return login(loginModel, response, clientIp,"ADMIN");
    }

  
}
