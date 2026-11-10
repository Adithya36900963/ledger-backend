package snovasys.ledger.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import snovasys.ledger.security.filter.UserVerificationFilter;
import snovasys.ledger.security.jwt.JWTAuthFilter;

        @Configuration
        public class WebSecurity {

        private final JWTAuthFilter jwtAuthFilter;
        private final UserVerificationFilter userVerificationFilter;

        public WebSecurity(
                JWTAuthFilter jwtAuthFilter,
                UserVerificationFilter userVerificationFilter) {

                this.jwtAuthFilter = jwtAuthFilter;
                this.userVerificationFilter =
                        userVerificationFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                HttpSecurity http)
                throws Exception {

                return http

                // ==========================================
                // CSRF
                // ==========================================

                .csrf(csrf ->
                        csrf.disable()
                )

                // ==========================================
                // CORS
                // ==========================================

                .cors(cors -> {})

                // ==========================================
                // SESSION
                // ==========================================

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // ==========================================
                // AUTHORIZATION
                // ==========================================

                .authorizeHttpRequests(auth -> auth

                        // --------------------------------------
                        // PUBLIC AUTH APIs
                        // --------------------------------------

                        .requestMatchers(HttpMethod.POST, "/api/users/auth/register")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/users/auth/**",
                                "/api/pincode/**"
                        ).permitAll()


                        // --------------------------------------
                        // SWAGGER
                        // --------------------------------------

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers("/api/health/**").permitAll()

                        // --------------------------------------
                        // ERROR
                        // --------------------------------------

                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        // --------------------------------------
                        // ADMIN CATEGORY
                        // --------------------------------------

                

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/category/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/category/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/category/**"
                        ).hasRole("ADMIN")

                        // Ledger endpoints also enforce operation-specific roles in services.
                        .requestMatchers("/api/audit/**")
                        .hasAnyRole("ADMIN", "AUDITOR")

                        .requestMatchers("/api/ledger/**")
                        .hasAnyRole("ADMIN", "ACCOUNT_HOLDER")

                        // --------------------------------------
                        // LOCATION
                        // --------------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/location/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/location/**"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/ACCOUNT_HOLDER/address/**").hasRole("ACCOUNT_HOLDER")
                        // --------------------------------------
                        // PINCODES
                        // --------------------------------------

                        
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pincodes/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/pincodes/**"
                        ).hasRole("ADMIN")

                        // --------------------------------------
                        // VENDOR ACTIVATION REQUEST
                        // --------------------------------------

                        .requestMatchers(
                                "/api/vendors/activation-request"
                        ).authenticated()

                        // --------------------------------------
                        // ADMIN APIs
                        // --------------------------------------
                        
                        .requestMatchers(
                                "/api/admin/**"
                        
                        ).hasRole("ADMIN")

                        // --------------------------------------
                        // ACCOUNT_HOLDER / VENDOR / ADMIN
                        // --------------------------------------

                        .requestMatchers(
                                "/api/address/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/orders/**"
                        ).authenticated()

                        .requestMatchers(
                                "/api/inventory/**"
                        ).authenticated()

                        // Razorpay calls this server-to-server, so there is no JWT.
                        // It is authenticated by the X-Razorpay-Signature HMAC instead
                        // (checked in PaymentServiceImpl.handleWebhook).
                        // Must stay ABOVE the /api/payments/** rule below.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/payments/webhook"
                        ).permitAll()

                        .requestMatchers(
                                "/api/payments/**"
                        ).authenticated()
                        .requestMatchers("/api/ACCOUNT_HOLDER/checkout").hasRole("ACCOUNT_HOLDER")

                        // --------------------------------------
                        // PUBLIC GET APIs
                        // --------------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/category/**",
                                "/api/product/details/**",
                                "/api/product/**",
                                "/api/sizes/**"
                        ).permitAll()

                        // --------------------------------------
                        // EVERYTHING ELSE
                        // --------------------------------------
                        .requestMatchers(HttpMethod.GET,"/api/ACCOUNT_HOLDER/wishlist/**").hasRole("ACCOUNT_HOLDER")
                        .requestMatchers(HttpMethod.POST,"/api/ACCOUNT_HOLDER/wishlist/**").hasRole("ACCOUNT_HOLDER")
                        .requestMatchers(HttpMethod.PATCH,"/api/ACCOUNT_HOLDER/wishlist/**").hasRole("ACCOUNT_HOLDER")
                        .requestMatchers(HttpMethod.DELETE,"/api/ACCOUNT_HOLDER/wishlist/**").hasRole("ACCOUNT_HOLDER")

                .requestMatchers("/api/cart/**").hasRole("ACCOUNT_HOLDER")
                        .anyRequest()
                        .hasAnyRole(
                                "ADMIN"
                        )
                )

                // ==========================================
                // JWT FIRST
                // ==========================================

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // ==========================================
                // VERIFICATION AFTER JWT
                // ==========================================

                .addFilterAfter(
                        userVerificationFilter,
                        JWTAuthFilter.class
                )

                .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {

                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                AuthenticationConfiguration config)
                throws Exception {

                return config.getAuthenticationManager();
        }
        }   
