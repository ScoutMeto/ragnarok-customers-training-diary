package com.ragnarok.ragnarok_customers_training_diary.configuration;

import com.ragnarok.ragnarok_customers_training_diary.service.AdminServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@Configuration
@Order(1)
public class AdminSecurityConfiguration {

    private final UserDetailsService userDetailsService;

    public AdminSecurityConfiguration(@Qualifier("adminUserDetailsService") UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/**") // vypnout CSRF pro API
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // frontend veřejně přístupný
                        .requestMatchers("/", "/index.html", "/js/**", "/css/**","/actuator/**").permitAll()
                        // veřejné API
                        .requestMatchers("/api/loginAdmin").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/loadAllTrainings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/createNewReservation").permitAll()
                        // chráněné API pro přihlášené adminy
                        .requestMatchers("/index-adminPart.html").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable()) // zcela vypnout přesměrování na /login
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/logoutAdmin"))
                        .logoutSuccessUrl("/index.html").permitAll()
                )
                .sessionManagement(session -> session.maximumSessions(10))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/index.html"))
                        .accessDeniedHandler((req, res, exc) -> res.sendRedirect("/index.html"))
                );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}