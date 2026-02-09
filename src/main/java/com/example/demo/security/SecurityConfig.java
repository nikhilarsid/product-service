package com.example.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 1. Public: View Products
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                        // 2. Public: Batch retrieval (used by Cart/Order services)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/batch").permitAll()

                        // ✅ 3. CRITICAL FIX: Allow Order Service to call reduce-stock without User Token
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/reduce-stock/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/migrate-usps").permitAll()
                        // 3. Restricted Access: Only Merchants can create new products
                        // This matches all OTHER POST requests to /products
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAuthority("ROLE_MERCHANT")

                        // 4. Merchant Only: Create/Update/Delete inventory
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAuthority("ROLE_MERCHANT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/inventory/**").hasAuthority("ROLE_MERCHANT")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/inventory/**").hasAuthority("ROLE_MERCHANT")

                        // 5. Everything else needs auth
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:3001",
                "https://ecom-frontend-simpl.vercel.app"  // ← ADD THIS LINE
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}