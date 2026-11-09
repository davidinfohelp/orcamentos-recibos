package com.empresa.os;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
@Controller
public class DocumentoController {
  private final DocumentoRepository repo; private final EmpresaRepository empresas; private final ClienteRepository clientes;
  public DocumentoController(DocumentoRepository r, EmpresaRepository e, ClienteRepository c){ repo=r; empresas=e; clientes=c; }
  private Documento meu(Long id, Principal p){
    return repo.findByIdAndDono(id, p.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
  /** Empresa do usuário logado (ou uma vazia, não salva, se ele ainda não configurou). */
  private Empresa empresaDe(Principal p){ return empresas.findByDono(p.getName()).orElseGet(Empresa::new); }
  private static String tipoItem(String t){ return "ITEM".equals(t) ? "ITEM" : "SERVICO"; }

  @GetMapping("/")
  public String lista(@RequestParam(required=false) String tipo, @RequestParam(required=false) String q, Model m, Principal p){
    if (p == null) return "home";   // visitante sem login vê a página inicial pública
    List<Documento> todos = repo.findByDonoOrderByIdDesc(p.getName());
    String busca = q==null ? "" : q.trim().toLowerCase();
    String digitos = busca.replaceAll("\\D","");
    List<Documento> docs = todos.stream()
      .filter(d -> tipo==null || tipo.isBlank() || d.getTipo().name().equals(tipo))
      .filter(d -> busca.isBlank() || (d.getCliente()!=null && d.getCliente().toLowerCase().contains(busca)) || String.valueOf(d.getNumero()).equals(busca)
                   || (!digitos.isEmpty() && d.getCpfCnpj()!=null && d.getCpfCnpj().replaceAll("\\D","").contains(digitos)))
      .collect(Collectors.toList());
    long qOrc = todos.stream().filter(d -> d.getTipo()==Documento.Tipo.ORCAMENTO).count();
    BigDecimal recebido = todos.stream().filter(d -> d.getTipo()==Documento.Tipo.RECIBO)
      .map(Documento::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal pendente = todos.stream().filter(d -> d.getTipo()==Documento.Tipo.ORCAMENTO && "Pendente".equals(d.getStatus()))
      .map(Documento::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    m.addAttribute("docs", docs); m.addAttribute("tipo", tipo); m.addAttribute("q", q);
    m.addAttribute("qOrc", qOrc); m.addAttribute("qRec", todos.size() - qOrc);
    m.addAttribute("recebido", recebido); m.addAttribute("pendente", pendente);
    m.addAttribute("empresa", empresaDe(p));
    return "lista";
  }
  @GetMapping("/novo")
  public String novo(@RequestParam(defaultValue="ORCAMENTO") Documento.Tipo tipo, @RequestParam(required=false) Long cliente, Model m, Principal p){
    Documento d = new Documento(); d.setTipo(tipo);
    if (tipo==Documento.Tipo.RECIBO) d.setStatus("Emitido");
    if (cliente != null) clientes.findByIdAndDono(cliente, p.getName()).ifPresent(c -> {
      d.setCliente(c.getNome()); d.setCpfCnpj(c.getCpfCnpj()); d.setTelefone(c.getTelefone());
    });
    for (int i=0;i<8;i++) d.getItens().add(new Item());
    m.addAttribute("d", d); m.addAttribute("empresa", empresaDe(p)); return "form";
  }
  @PostMapping("/salvar")
  public String salvar(@ModelAttribute Documento d, @RequestParam(defaultValue="false") boolean salvarCliente, Principal p){
    List<Item> ok = new ArrayList<>();
    for (Item i : d.getItens()) if (i.getDescricao()!=null && !i.getDescricao().isBlank()) {
      if (i.getQuantidade()==null) i.setQuantidade(1);
      if (i.getValorUnitario()==null) i.setValorUnitario(BigDecimal.ZERO);
      i.setTipo(tipoItem(i.getTipo()));
      ok.add(i);
    }
    d.setItens(ok); d.setId(null); d.setDono(p.getName());
    d.setNumero(repo.maxNumero(d.getTipo(), p.getName())+1);
    if (salvarCliente) guardarCliente(d, p.getName());
    return "redirect:/ver/" + repo.save(d).getId();
  }
  /** Cadastra o cliente do documento na base (se ainda não existir) ou completa os dados que estavam em branco. */
  private void guardarCliente(Documento d, String dono){
    String nome = ClienteController.limpa(d.getCliente(), 150);
    if (nome == null) return;
    String dig = Cliente.digitos(d.getCpfCnpj());
    if (!dig.isEmpty() && dig.length() != 11 && dig.length() != 14) dig = "";
    Optional<Cliente> achado = !dig.isEmpty() ? clientes.findFirstByDonoAndDocumento(dono, dig) : clientes.findFirstByDonoAndNomeIgnoreCase(dono, nome);
    Cliente c = achado.orElseGet(() -> { Cliente n = new Cliente(); n.setDono(dono); n.setNome(nome); return n; });
    if (c.getDocumento() == null && !dig.isEmpty()) c.setCpfCnpj(dig);
    String tel = ClienteController.limpa(d.getTelefone(), 30);
    if (tel != null && (c.getTelefone() == null || c.getTelefone().isBlank())) c.setTelefone(tel);
    clientes.save(c);
  }
  @GetMapping("/ver/{id}")
  public String ver(@PathVariable Long id, Model m, Principal p){
    m.addAttribute("d", meu(id, p)); m.addAttribute("empresa", empresaDe(p)); return "ver";
  }
  @PostMapping("/status/{id}")
  public String status(@PathVariable Long id, @RequestParam String status, Principal p){
    Documento d = meu(id, p); d.setStatus(status); repo.save(d); return "redirect:/";
  }
  @PostMapping("/converter/{id}")
  public String converter(@PathVariable Long id, Principal p){
    Documento o = meu(id, p); Documento r = new Documento();
    r.setTipo(Documento.Tipo.RECIBO); r.setStatus("Emitido"); r.setDono(p.getName());
    r.setCliente(o.getCliente()); r.setTelefone(o.getTelefone()); r.setCpfCnpj(o.getCpfCnpj());
    r.setObservacoes("Referente ao orçamento nº " + o.getNumero()); r.setData(LocalDate.now());
    for (Item i : o.getItens()) { Item c = new Item(); c.setDescricao(i.getDescricao()); c.setQuantidade(i.getQuantidade()); c.setValorUnitario(i.getValorUnitario()); c.setTipo(tipoItem(i.getTipo())); r.getItens().add(c); }
    r.setNumero(repo.maxNumero(Documento.Tipo.RECIBO, p.getName())+1); o.setStatus("Aprovado"); repo.save(o);
    return "redirect:/ver/" + repo.save(r).getId();
  }
  @PostMapping("/excluir/{id}")
  public String excluir(@PathVariable Long id, Principal p){ repo.delete(meu(id, p)); return "redirect:/"; }
}
