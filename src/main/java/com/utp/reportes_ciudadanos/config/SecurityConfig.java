package com.utp.reportes_ciudadanos.config;
import com.utp.reportes_ciudadanos.service.RolService;
import com.utp.reportes_ciudadanos.service.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final UserDetailsServiceImpl userDetailsService;
    private final RolService rolService;
    private final DynamicAuthorizationManager dynamicAuthorizationManager;

    @Autowired
    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
            RolService rolService, DynamicAuthorizationManager dynamicAuthorizationManager) {
        this.userDetailsService = userDetailsService;
        this.rolService = rolService;
        this.dynamicAuthorizationManager = dynamicAuthorizationManager;
    }

    // ── BEANS DE SEGURIDAD ─────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider
                = new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── SECURITY FILTER CHAIN DINÁMICO ────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> {

                    // ── Siempre públicas ──
                    auth.requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll();
                    auth.requestMatchers("/", "/login", "/registro", "/error/**").permitAll();

                    // ── Reglas dinámicas por rol ──
                    auth.anyRequest()
                        .access(dynamicAuthorizationManager);
                })
                // ── Formulario de Login ────────────────────────────────────
                .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
                )
                // ── Logout ─────────────────────────────────────────────────
                .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
                )
                // ── Acceso denegado ────────────────────────────────────────
                .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
                );

        return http.build();
    }

    // ── SUCCESS HANDLER ────────────────────────────────────────────────
    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            boolean esAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            response.sendRedirect(esAdmin ? "/admin" : "/dashboard");
        };
    }
}

