package com.finshield.backend.config;

import com.finshield.backend.auth.config.JwtProperties;
import com.finshield.backend.auth.security.FinshieldUserDetailsService;
import com.finshield.backend.auth.security.JwtAuthenticationFilter;
import com.finshield.backend.auth.security.RestAccessDeniedHandler;
import com.finshield.backend.auth.security.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/ws/**",
            "/actuator/health",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/admin/**", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/fraud/**").hasAnyRole(
                                "ADMIN", "FRAUD_ANALYST", "AML_INVESTIGATOR",
                                "COMPLIANCE_OFFICER", "RISK_MANAGER")
                        .requestMatchers("/api/aml/**").hasAnyRole(
                                "ADMIN", "AML_INVESTIGATOR", "COMPLIANCE_OFFICER", "RISK_MANAGER")
                        .requestMatchers("/api/cases/**").hasAnyRole(
                                "ADMIN", "FRAUD_ANALYST", "AML_INVESTIGATOR",
                                "COMPLIANCE_OFFICER", "RISK_MANAGER")
                        .requestMatchers("/api/customers/**", "/api/accounts/**").hasAnyRole(
                                "ADMIN", "FRAUD_ANALYST", "AML_INVESTIGATOR",
                                "COMPLIANCE_OFFICER", "RISK_MANAGER")
                        .requestMatchers("/api/transactions/**").hasAnyRole(
                                "ADMIN", "FRAUD_ANALYST", "AML_INVESTIGATOR",
                                "COMPLIANCE_OFFICER", "RISK_MANAGER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(
            FinshieldUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return new ProviderManager(provider);
    }
}
