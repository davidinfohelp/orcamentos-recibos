package com.empresa.os;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.*;

@Controller
public class ClienteController {
  private final ClienteRepository repo; private final CnpjService cnpj;
  public ClienteController(ClienteRepository r, CnpjService c){ repo = r; cnpj = c; }

  static String limpa(String s, int max){
    if (s == null) return null;
    String t = s.trim();
    if (t.isEmpty()) return null;
    return t.length() > max ? t.substring(0, max) : t;
  }
  private Cliente meu(Long id, Principal p){
    return repo.findByIdAndDono(id, p.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/clientes")
  public String lista(@RequestParam(required=false) String q, Model m, Principal p){
    List<Cliente> cs = (q == null || q.isBlank()) ? repo.findByDonoOrderByNomeAsc(p.getName()) : repo.buscar(p.getName(), q);
    m.addAttribute("clientes", cs); m.addAttribute("q", q);
    return "clientes";
  }

  @GetMapping("/clientes/novo")
  public String novo(Model m){ m.addAttribute("c", new Cliente()); return "cliente-form"; }

  @GetMapping("/clientes/{id}")
  public String editar(@PathVariable Long id, Model m, Principal p){ m.addAttribute("c", meu(id, p)); return "cliente-form"; }

  @PostMapping("/clientes/salvar")
  public String salvar(@RequestParam(required=false) Long id, @RequestParam(defaultValue="") String nome,
                       @RequestParam(defaultValue="") String nomeFantasia, @RequestParam(defaultValue="") String cpfCnpj,
                       @RequestParam(defaultValue="") String telefone, @RequestParam(defaultValue="") String email,
                       @RequestParam(defaultValue="") String endereco, @RequestParam(defaultValue="") String situacao,
                       @RequestParam(defaultValue="") String observacoes, Model m, Principal p){
    // id só serve para localizar um cliente DO USUÁRIO LOGADO; o dono nunca vem do formulário.
    Cliente c = id == null ? new Cliente() : meu(id, p);
    c.setDono(p.getName());
    c.setNome(limpa(nome, 150)); c.setNomeFantasia(limpa(nomeFantasia, 150)); c.setCpfCnpj(limpa(cpfCnpj, 30));
    c.setTelefone(limpa(telefone, 30)); c.setEmail(limpa(email, 120)); c.setEndereco(limpa(endereco, 250));
    c.setSituacao(limpa(situacao, 40)); c.setObservacoes(limpa(observacoes, 1000));
    String erro = null;
    String dig = Cliente.digitos(cpfCnpj);
    if (c.getNome() == null) erro = "Informe o nome / razão social do cliente.";
    else if (!dig.isEmpty() && dig.length() != 11 && dig.length() != 14) erro = "CPF deve ter 11 dígitos e CNPJ 14.";
    else if (dig.length() == 14 && !CnpjService.cnpjValido(dig)) erro = "CNPJ inválido (dígitos verificadores não conferem).";
    else if (!dig.isEmpty()) {
      Optional<Cliente> outro = repo.findFirstByDonoAndDocumento(p.getName(), dig);
      if (outro.isPresent() && !outro.get().getId().equals(c.getId())) erro = "Já existe um cliente com esse CPF/CNPJ: " + outro.get().getNome() + ".";
    }
    if (erro != null) { m.addAttribute("erro", erro); m.addAttribute("c", c); return "cliente-form"; }
    repo.save(c);
    return "redirect:/clientes?salvo";
  }

  @PostMapping("/clientes/{id}/excluir")
  public String excluir(@PathVariable Long id, Principal p){ repo.delete(meu(id, p)); return "redirect:/clientes?excluido"; }

  /** Busca rápida usada pelo formulário de orçamento/recibo. */
  @GetMapping("/api/clientes") @ResponseBody
  public List<Map<String, String>> buscar(@RequestParam(defaultValue="") String q, Principal p){
    if (q.trim().length() < 2) return List.of();
    List<Map<String, String>> out = new ArrayList<>();
    for (Cliente c : repo.buscar(p.getName(), q)) {
      if (out.size() >= 10) break;
      Map<String, String> m = new LinkedHashMap<>();
      m.put("id", String.valueOf(c.getId())); m.put("nome", c.getNome());
      m.put("nomeFantasia", Objects.toString(c.getNomeFantasia(), "")); m.put("cpfCnpj", Objects.toString(c.getCpfCnpj(), ""));
      m.put("telefone", Objects.toString(c.getTelefone(), ""));
      out.add(m);
    }
    return out;
  }

  /** Consulta os dados públicos de um CNPJ (Receita, via BrasilAPI / CNPJ.ws). */
  @GetMapping("/api/cnpj/{numero}") @ResponseBody
  public ResponseEntity<Map<String, String>> consultarCnpj(@PathVariable String numero){
    try {
      return cnpj.consultar(numero).map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "CNPJ não encontrado na Receita.")));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("erro", e.getMessage()));
    }
  }
}
