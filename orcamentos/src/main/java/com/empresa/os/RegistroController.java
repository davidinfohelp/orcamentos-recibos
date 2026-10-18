package com.empresa.os;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Controller
public class RegistroController {
  private final UsuarioRepository repo; private final PasswordEncoder encoder; private final String convite;
  public RegistroController(UsuarioRepository r, PasswordEncoder e, @Value("${CODIGO_CONVITE:troque-este-convite}") String convite){
    repo=r; encoder=e; this.convite=convite;
  }
  @GetMapping("/login") public String login(){ return "login"; }
  @GetMapping("/registro") public String form(){ return "registro"; }
  @PostMapping("/registro")
  public String criar(@RequestParam String username, @RequestParam String password, @RequestParam String confirma,
                      @RequestParam(defaultValue="") String codigo, Model m){
    String u = username.trim().toLowerCase(); String erro = null;
    boolean conviteOk = MessageDigest.isEqual(codigo.trim().getBytes(StandardCharsets.UTF_8), convite.getBytes(StandardCharsets.UTF_8));
    if (!conviteOk) erro = "Código de convite inválido.";
    else if (!u.matches("[a-z0-9._-]{3,30}")) erro = "Usuário: 3 a 30 caracteres (letras, números, ponto, hífen ou _).";
    else if (password.length() < 8) erro = "A senha precisa ter pelo menos 8 caracteres.";
    else if (!password.equals(confirma)) erro = "As senhas não conferem.";
    else if (repo.existsByUsername(u)) erro = "Esse usuário já existe.";
    if (erro != null) { m.addAttribute("erro", erro); m.addAttribute("username", username); return "registro"; }
    Usuario n = new Usuario(); n.setUsername(u); n.setPassword(encoder.encode(password)); repo.save(n);
    return "redirect:/login?registrado";
  }
}
