package com.cinema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RoleBasedAuthSuccessHandler successHandler;

    public SecurityConfig(RoleBasedAuthSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // публична програма за landing страницата
                        .requestMatchers(HttpMethod.GET,
                                "/api/movies/**",
                                "/api/cinemas/**",
                                "/api/projections/**",
                                "/api/halls/**",
                                "/api/products"
                        ).permitAll()
                        .requestMatchers(
                                "/admin.html",
                                "/admin-cinemas.html",
                                "/admin-movies.html",
                                "/admin-program.html",
                                "/admin-bar.html",
                                "/admin-staff.html",
                                "/admin-reports.html"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/cashier.html",
                                "/cashier-program.html",
                                "/cashier-bar.html"
                        ).hasAnyRole("CASHIER", "ADMIN")
                        .requestMatchers("/api/cashier/**").hasAnyRole("CASHIER", "ADMIN")
                        .requestMatchers(
                                "/client.html",
                                "/booking.html",
                                "/account.html",
                                "/tickets.html",
                                "/balance.html",
                                "/menu.html",
                                "/test-api.html"
                        ).hasAnyRole("CLIENT", "CASHIER", "ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/index.html")
                        .loginProcessingUrl("/login")
                        .failureUrl("/index.html?error=1")
                        .successHandler(successHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/index.html?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
