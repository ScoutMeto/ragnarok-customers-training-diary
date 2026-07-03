package com.ragnarok.ragnarok_customers_training_diary.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SwitchUserFilter switchUserFilter) throws Exception {
        http
                .addFilterAfter(switchUserFilter, AuthorizationFilter.class)
                // /admin/impersonate je GET (CSRF se GETu netýká); /impersonate/exit povolíme
                .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**")))
                .authorizeHttpRequests(auth -> auth
                        // Statika a veřejné stránky
                        .requestMatchers("/", "/login", "/register", "/error").permitAll()
                        .requestMatchers("/confirm-email", "/confirm-email/**").permitAll()
                        .requestMatchers("/forgot-password", "/reset-password").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/webjars/**", "/images/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // API endpointy pro registraci (anonymous přístup)
                        .requestMatchers("/api/auth/register").permitAll()
                        // Admin sekce
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        // Admin akce mimo /admin/** prefix (defense in depth k role checku v service)
                        .requestMatchers("/reservations/admin-cancel").hasRole("ADMIN")
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

    /**
     * Phase 20c (ScoutMeto): impersonace — admin se „přepne" na uživatele a vidí jeho
     * deník/statistiky s plnými právy (1:1, bez stopy). Po přepnutí přesměruje dle
     * parametru {@code next} (diary/analysis). Návrat přes {@code /impersonate/exit}.
     */
    @Bean
    public SwitchUserFilter switchUserFilter(UserDetailsService userDetailsService) {
        SwitchUserFilter filter = new SwitchUserFilter();
        filter.setUserDetailsService(userDetailsService);
        // GET odkazy z pickeru → matchery na GET (default SwitchUserFilteru je POST)
        filter.setSwitchUserMatcher(new AntPathRequestMatcher("/admin/impersonate", "GET"));
        filter.setExitUserMatcher(new AntPathRequestMatcher("/impersonate/exit", "GET"));
        filter.setSwitchFailureUrl("/admin/accounts?impersonateError");
        filter.setSuccessHandler((request, response, authentication) -> {
            // Stejný handler běží pro switch i exit. Při switchi má auth ROLE_PREVIOUS_ADMINISTRATOR
            // (vznikne při přepnutí na uživatele); při exitu je zpět admin bez ní.
            boolean isSwitch = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_PREVIOUS_ADMINISTRATOR".equals(a.getAuthority()));
            if (isSwitch) {
                String next = request.getParameter("next");
                response.sendRedirect("analysis".equals(next) ? "/analysis" : "/diary");
            } else {
                // Po „Ukončit náhled" zpět na výběr uživatele (ScoutMeto kolo 6),
                // ne na admin-ův prázdný deník.
                response.sendRedirect("/admin/impersonate-select?next=diary");
            }
        });
        return filter;
    }
}
