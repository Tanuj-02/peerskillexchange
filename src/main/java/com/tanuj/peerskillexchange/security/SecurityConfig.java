package com.tanuj.peerskillexchange.security;

import com.tanuj.peerskillexchange.security.jwt.jwtFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailService customUserDetailService;
    private final jwtFilter jwtFilter;

    public SecurityConfig(CustomUserDetailService customUserDetailService, jwtFilter jwtFilter) {
        this.customUserDetailService = customUserDetailService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers("/health").permitAll()
                                .requestMatchers("/api/auth/**").permitAll() // Covers login and register
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/skills").permitAll() // Publicly view skills
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/doc",
                                        "/error",
                                        "/ws/**",
                                        "/ws"
                                ).permitAll()

                                .requestMatchers("/actuator/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "USER", "ROLE_USER")
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/skills").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                                .requestMatchers(
                                        "/api/requests/**",
                                        "/api/users",
                                        "/api/users/**",
                                        "/api/discover",
                                        "/api/discover/**",
                                        "/api/analytics/**"
                                ).hasAnyAuthority("USER", "ROLE_USER", "ADMIN", "ROLE_ADMIN")

                                .anyRequest().authenticated()
                )
//                .httpBasic(Customizer.withDefaults());
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder(14);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(customUserDetailService);
        daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration){
        return configuration.getAuthenticationManager();
    }

    @Value("${FRONTEND_URL}")
    private String frontendUrl;
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(frontendUrl, "http://localhost:3000"));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
