package com.empresa.os;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.regex.Pattern;

@Controller
public class EmpresaController {
  private static final Pattern COR = Pattern.compile("^#[0-9a-fA-F]{6}$");
  private static final Pattern LOGO = Pattern.compile("^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$");
  private static final int LOGO_MAX = 350_000;
  private final EmpresaRepository repo;
  public EmpresaController(EmpresaRepository r){ repo = r; }

  private static String limpa(String s, int max){
    if (s == null) return null;
    String t = s.trim();
    if (t.isEmpty()) return null;
    return t.length() > max ? t.substring(0, max) : t;
  }

  @GetMapping("/empresa")
  public String form(Model m, Principal p){
    m.addAttribute("e", repo.findByDono(p.getName()).orElseGet(Empresa::new));
    return "empresa";
  }

  @PostMapping("/empresa")
  public String salvar(@RequestParam(defaultValue="") String nome, @RequestParam(defaultValue="") String cpfCnpj,
                       @RequestParam(defaultValue="") String telefone, @RequestParam(defaultValue="") String email,
                       @RequestParam(defaultValue="") String endereco, @RequestParam(defaultValue="") String cor,
                       @RequestParam(defaultValue="") String logo, @RequestParam(defaultValue="false") boolean removerLogo,
                       Model m, Principal p){
    // Sempre busca/cria a empresa DO USUÁRIO LOGADO: nunca recebe id nem dono pelo formulário.
    Empresa e = repo.findByDono(p.getName()).orElseGet(() -> { Empresa n = new Empresa(); n.setDono(p.getName()); return n; });
    String erro = null;
    String nomeOk = limpa(nome, 120);
    if (nomeOk == null) erro = "Informe o nome da empresa.";
    String logoNovo = logo.trim();
    if (erro == null && !removerLogo && !logoNovo.isEmpty()) {
      if (logoNovo.length() > LOGO_MAX || !LOGO.matcher(logoNovo).matches()) erro = "Logo inválida. Use uma imagem PNG, JPG ou WEBP.";
    }
    e.setNome(nomeOk); e.setCpfCnpj(limpa(cpfCnpj, 30)); e.setTelefone(limpa(telefone, 30));
    e.setEmail(limpa(email, 120)); e.setEndereco(limpa(endereco, 200));
    e.setCor(COR.matcher(cor.trim()).matches() ? cor.trim() : "#1f6feb");
    if (erro != null) { m.addAttribute("erro", erro); m.addAttribute("e", e); return "empresa"; }
    if (removerLogo) e.setLogo(null); else if (!logoNovo.isEmpty()) e.setLogo(logoNovo);
    repo.save(e);
    return "redirect:/empresa?ok";
  }
}
