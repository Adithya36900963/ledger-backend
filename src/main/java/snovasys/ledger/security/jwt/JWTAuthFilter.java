package snovasys.ledger.security.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.repository.UserRepo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserRepo ur;

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        String path = request.getServletPath();

        return path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/v3/api-docs")
                || path.equals("/api/users/auth/login")
                || path.equals("/api/users/auth/register")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        // ==========================================
        // GET JWT FROM COOKIE
        // ==========================================

        if (request.getCookies() != null) {

            for (Cookie cookie : request.getCookies()) {

                if ("jwt".equals(cookie.getName())) {

                    token = cookie.getValue();
                    break;
                }
            }
        }

        // ==========================================
        // AUTHENTICATE USER
        // ==========================================

        if (token != null
                && SecurityContextHolder
                        .getContext()
                        .getAuthentication() == null) {

            String email =
                    jwtUtil.extractSubjectOrNull(token);

            if (email != null) {

                User user =
                        ur.findByEmailOrNumber(email);

                if (user != null) {

                    List<SimpleGrantedAuthority> authorities =
                            List.of(
                                new SimpleGrantedAuthority(
                                    "ROLE_" + user.getRole()
                                )
                            );

                    /*
                     * IMPORTANT:
                     *
                     * Put USER as principal,
                     * not email.
                     */
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    authorities
                            );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication
                            );
                }
            }
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}