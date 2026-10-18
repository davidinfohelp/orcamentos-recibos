package com.empresa.os;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
  @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a.requestMatchers("/login", "/registro", "/logo.png", "/error").permitAll().anyRequest().authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
        .logout(l -> l.logoutSuccessUrl("/login?saiu"));
    return http.build();
  }
  @Bean PasswordEncoder encoder(){ return new BCryptPasswordEncoder(); }
  @Bean UserDetailsService users(UsuarioRepository repo){
    return nome -> repo.findByUsername(nome.trim().toLowerCase())
      .map(u -> User.withUsername(u.getUsername()).password(u.getPassword()).roles("USER").build())
      .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
  }
}
