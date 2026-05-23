package com.ragnarok.ragnarok_customers_training_diary.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Jediná security konfigurace (sjednoceno User + Admin). Autorizace probíhá podle role
 * na {@link com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity}.
 *
 * <p>Form-login na {@code /login}, session-based. Pro REST API endpointy ({@code /api/**})
 * je CSRF vypnutý (volá je JS přes fetch). Pro Thymeleaf formuláře je CSRF zapnutý.
 *
 * <p>{@code AuthenticationManager} se autokonfiguruje Spring Bootem ze
 * {@link com.ragnarok.ragnarok_customers_training_diary.account.AccountUserDetailsService}
 * + {@link #passwordEncoder()} bean — žádný custom {@code DaoAuthenticationProvider} není potřeba.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**")))
                .authorizeHttpRequests(auth -> auth
                        // Statika a veřejné stránky
                        .requestMatchers("/", "/login", "/register", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/webjars/**", "/images/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // API endpointy pro registraci (anonymous přístup)
                        .requestMatchers("/api/auth/register").permitAll()
                        // Admin sekce
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        // Vše ostatní vyžaduje přihlášení
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session.maximumSessions(10));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
