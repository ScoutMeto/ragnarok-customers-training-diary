package com.ragnarok.ragnarok_customers_training_diary.configuration;

import com.ragnarok.ragnarok_customers_training_diary.service.AdminServiceImpl;
import com.ragnarok.ragnarok_customers_training_diary.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

//nezbytné celou třídu upravit - AdminSecurityConfiguration je okopírované z Rezervačního systému, tahle je pouze nástřel
@Configuration
@Order(2)
public class UserSecurityConfiguration {

    private final UserDetailsService userDetailsService;

    public UserSecurityConfiguration(@Qualifier("userUserDetailsService") UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
        http
//                .csrf(csrf -> csrf
//                        .ignoringRequestMatchers(("/api/**") // vypnout CSRF pro API
//                        )
//                )
                .securityMatcher("/index-userPart.html", "/api/loginUser", "/api/user/**")
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                                // frontend veřejně přístupný
                                .requestMatchers("/", "/index.html", "/js/**", "/css/**").permitAll()
                                // veřejné API
                                .requestMatchers("/api/loginUser").permitAll()
//                        .requestMatchers(HttpMethod.GET, "/api/loadAllTrainings").permitAll()
//                        .requestMatchers(HttpMethod.POST, "/api/createNewReservation").permitAll()
                                // chráněné API pro přihlášené adminy
                                .requestMatchers("/index-userPart.html").hasRole("USER")
                                .requestMatchers("/api/**").hasRole("USER")
                                .anyRequest().authenticated()
                )
                .userDetailsService(userDetailsService)
                .formLogin(form -> form.disable()) // zcela vypnout přesměrování na /login
                .logout(logout -> logout
                        .logoutUrl(("/api/logout"))
                        .logoutSuccessUrl("/index.html").permitAll()
                )
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