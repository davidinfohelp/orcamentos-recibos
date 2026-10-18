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
  private final DocumentoRepository repo;
  public DocumentoController(DocumentoRepository r){ repo=r; }
  private Documento meu(Long id, Principal p){
    return repo.findByIdAndDono(id, p.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
  @GetMapping("/")
  public String lista(@RequestParam(required=false) String tipo, @RequestParam(required=false) String q, Model m, Principal p){
    String busca = q==null ? "" : q.toLowerCase();
    List<Documento> docs = repo.findByDonoOrderByIdDesc(p.getName()).stream()
      .filter(d -> tipo==null || tipo.isBlank() || d.getTipo().name().equals(tipo))
      .filter(d -> busca.isBlank() || (d.getCliente()!=null && d.getCliente().toLowerCase().contains(busca)) || String.valueOf(d.getNumero()).equals(busca))
      .collect(Collectors.toList());
    m.addAttribute("docs", docs); m.addAttribute("tipo", tipo); m.addAttribute("q", q);
    return "lista";
  }
  @GetMapping("/novo")
  public String novo(@RequestParam(defaultValue="ORCAMENTO") Documento.Tipo tipo, Model m){
    Documento d = new Documento(); d.setTipo(tipo);
    if (tipo==Documento.Tipo.RECIBO) d.setStatus("Emitido");
    for (int i=0;i<8;i++) d.getItens().add(new Item());
    m.addAttribute("d", d); return "form";
  }
  @PostMapping("/salvar")
  public String salvar(@ModelAttribute Documento d, Principal p){
    List<Item> ok = new ArrayList<>();
    for (Item i : d.getItens()) if (i.getDescricao()!=null && !i.getDescricao().isBlank()) {
      if (i.getQuantidade()==null) i.setQuantidade(1);
      if (i.getValorUnitario()==null) i.setValorUnitario(BigDecimal.ZERO);
      ok.add(i);
    }
    d.setItens(ok); d.setId(null); d.setDono(p.getName());
    d.setNumero(repo.maxNumero(d.getTipo(), p.getName())+1);
    return "redirect:/ver/" + repo.save(d).getId();
  }
  @GetMapping("/ver/{id}")
  public String ver(@PathVariable Long id, Model m, Principal p){ m.addAttribute("d", meu(id, p)); return "ver"; }
  @PostMapping("/status/{id}")
  public String status(@PathVariable Long id, @RequestParam String status, Principal p){
    Documento d = meu(id, p); d.setStatus(status); repo.save(d); return "redirect:/";
  }
  @PostMapping("/converter/{id}")
  public String converter(@PathVariable Long id, Principal p){
    Documento o = meu(id, p); Documento r = new Documento();
    r.setTipo(Documento.Tipo.RECIBO); r.setStatus("Emitido"); r.setDono(p.getName());
    r.setCliente(o.getCliente()); r.setTelefone(o.getTelefone());
    r.setObservacoes("Referente ao orçamento nº " + o.getNumero()); r.setData(LocalDate.now());
    for (Item i : o.getItens()) { Item c = new Item(); c.setDescricao(i.getDescricao()); c.setQuantidade(i.getQuantidade()); c.setValorUnitario(i.getValorUnitario()); r.getItens().add(c); }
    r.setNumero(repo.maxNumero(Documento.Tipo.RECIBO, p.getName())+1); o.setStatus("Aprovado"); repo.save(o);
    return "redirect:/ver/" + repo.save(r).getId();
  }
  @PostMapping("/excluir/{id}")
  public String excluir(@PathVariable Long id, Principal p){ repo.delete(meu(id, p)); return "redirect:/"; }
}
