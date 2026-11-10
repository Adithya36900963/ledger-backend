package snovasys.ledger.security.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import snovasys.ledger.users.entity.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserVerificationFilter
        extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        String path = request.getServletPath();

        /*
         * Authentication and verification APIs
         * should not be blocked by this filter.
         */
        return path.startsWith("/api/users/auth/")
                || path.startsWith("/api/health/")

                || path.equals("/api/users/auth/email")
                || path.startsWith("/api/users/auth/email/")

                || path.equals("/api/users/auth/mobile")
                || path.startsWith("/api/users/auth/mobile/")

                /*
                 * Swagger
                 */
                || path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/v3/api-docs")

                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        /*
         * If user isn't authenticated,
         * let Spring Security handle it.
         */
        if (authentication == null
                || !authentication.isAuthenticated()) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        /*
         * JWTAuthFilter should put User
         * as the principal.
         */
        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof User)) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        User user = (User) principal;

        // ==========================================
        // EMAIL VERIFICATION
        // ==========================================

        boolean emailVerified =
                Boolean.TRUE.equals(
                        user.getEmailVerified()
                );

        // ==========================================
        // MOBILE VERIFICATION
        // ==========================================

        boolean mobileVerified =
                Boolean.TRUE.equals(
                        user.getMobileVerified()
                );

        /*
         * Both email and mobile must be verified
         * before accessing protected APIs.
         */
        if (!emailVerified || !mobileVerified) {

            sendVerificationResponse(
                    response,
                    emailVerified,
                    mobileVerified
            );

            return;
        }

        /*
         * Roles currently supported by this application are ADMIN,
         * ACCOUNT_HOLDER, and AUDITOR. Verification is an account-state
         * check; role authorization remains enforced by Spring Security
         * and operation-specific service checks.
         */

        filterChain.doFilter(
                request,
                response
        );
    }

    // ==========================================
    // VERIFICATION RESPONSE
    // ==========================================

    private void sendVerificationResponse(
            HttpServletResponse response,
            boolean emailVerified,
            boolean mobileVerified)
            throws IOException {

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

        response.setContentType(
                "application/json"
        );

        Map<String, Object> res =
                new HashMap<>();

        res.put(
                "success",
                false
        );

        res.put(
                "message",
                "Account verification required"
        );

        res.put(
                "isEmailVerified",
                emailVerified
        );

        res.put(
                "isNumberVerified",
                mobileVerified
        );

        objectMapper.writeValue(
                response.getWriter(),
                res
        );
    }
}
