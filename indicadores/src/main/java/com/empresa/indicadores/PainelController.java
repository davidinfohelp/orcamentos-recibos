package com.empresa.indicadores;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class PainelController {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PainelController.class);
  private final ImportacaoRepository imps; private final RegistroRepository regs;
  private final Indicadores ind; private final ImportacaoService service;
  public PainelController(ImportacaoRepository i, RegistroRepository r, Indicadores in, ImportacaoService s){ imps = i; regs = r; ind = in; service = s; }

  /** Cartão de uma área no painel. */
  public static class Card {
    private final Area area; private final Importacao imp; private final Collection<Indicadores.Linha> linhas;
    Card(Area a, Importacao i, Collection<Indicadores.Linha> l){ area = a; imp = i; linhas = l; }
    public Area getArea(){return area;} public Importacao getImp(){return imp;} public Collection<Indicadores.Linha> getLinhas(){return linhas;}
  }
  /** Linha do histórico: um mês, com o resultado de cada área. */
  public static class Mes {
    private final int ano, mes; private final Map<Area, Collection<Indicadores.Linha>> areas = new EnumMap<>(Area.class);
    Mes(int a, int m){ ano = a; mes = m; }
    public int getAno(){return ano;} public int getMes(){return mes;}
    public String getRotulo(){ return Meses.nome(mes) + "/" + ano; }
    public Map<Area, Collection<Indicadores.Linha>> getAreas(){return areas;}
  }

  private static Map<String, String> opcao(int a, int m){ return Map.of("valor", a + "-" + m, "rotulo", Meses.nome(m) + "/" + a); }

  @GetMapping("/")
  public String painel(@RequestParam(required = false) Integer ano, @RequestParam(required = false) Integer mes, Model model){
    List<int[]> periodos = ind.periodos();
    if (periodos.isEmpty()) return "redirect:/importar";
    if (ano == null || mes == null) { ano = periodos.get(0)[0]; mes = periodos.get(0)[1]; }
    Map<Long, Map<String, Indicadores.Linha>> resumo = ind.resumoDeTodas();
    List<Card> cards = new ArrayList<>();
    for (Area a : Area.values()) {
      Importacao i = imps.findByAreaAndAnoAndMes(a, ano, mes).orElse(null);
      cards.add(new Card(a, i, i == null ? List.of() : resumo.getOrDefault(i.getId(), Map.of()).values()));
    }
    Map<String, Mes> hist = new LinkedHashMap<>();
    for (Importacao i : imps.findAllByOrderByAnoDescMesDescAreaAsc())
      hist.computeIfAbsent(i.getAno() + "-" + i.getMes(), k -> new Mes(i.getAno(), i.getMes()))
          .areas.put(i.getArea(), resumo.getOrDefault(i.getId(), Map.of()).values());
    model.addAttribute("periodos", periodos.stream().map(p -> opcao(p[0], p[1])).toList());
    model.addAttribute("atual", ano + "-" + mes); model.addAttribute("rotulo", Meses.nome(mes) + "/" + ano);
    model.addAttribute("ano", ano); model.addAttribute("mes", mes);
    model.addAttribute("cards", cards); model.addAttribute("historico", hist.values()); model.addAttribute("areas", Area.values());
    return "painel";
  }

  private Importacao buscar(Area area, int ano, int mes){
    return imps.findByAreaAndAnoAndMes(area, ano, mes).orElseThrow(() ->
      new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Nada importado de " + area.getNome() + " em " + Meses.nome(mes) + "/" + ano));
  }

  @GetMapping("/area/{area}")
  public String area(@PathVariable Area area, @RequestParam int ano, @RequestParam int mes,
                     @RequestParam(defaultValue = "fora") String ver, Model model){
    Importacao imp = buscar(area, ano, mes);
    List<Registro> rs = regs.findByImportacaoOrderByIdAsc(imp);
    model.addAttribute("area", area); model.addAttribute("imp", imp); model.addAttribute("ano", ano); model.addAttribute("mes", mes);
    List<String> faixas = ind.faixas(rs);
    model.addAttribute("faixas", faixas);
    model.addAttribute("mostrarFora", faixas.stream().noneMatch(f -> LeitorPlanilha.norm(f).startsWith("fora")));
    model.addAttribute("classes", ind.porClassificacao(rs));
    model.addAttribute("grupos", ind.porGrupo(rs));
    boolean todos = "todos".equals(ver);
    List<Registro> lista = todos ? rs : rs.stream().filter(r -> !r.isDentroPrazo()).toList();
    model.addAttribute("ver", todos ? "todos" : "fora");
    model.addAttribute("lista", lista.size() > 3000 ? lista.subList(0, 3000) : lista);
    model.addAttribute("totalLista", lista.size());
    return "area";
  }

  /** Baixa todas as linhas da área/mês em CSV (abre direto no Excel). */
  @GetMapping("/area/{area}/csv")
  public ResponseEntity<byte[]> csv(@PathVariable Area area, @RequestParam int ano, @RequestParam int mes){
    Importacao imp = buscar(area, ano, mes);
    DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    StringBuilder sb = new StringBuilder("﻿");
    sb.append("Classificação;Faixa de prazo;Situação;").append(area.getGrupo()).append(";Solicitação;Descrição;Responsável;Início;Fim;Observação\r\n");
    for (Registro r : regs.findByImportacaoOrderByIdAsc(imp)) {
      String[] c = { r.getClassificacao(), r.getFaixa(), r.isDentroPrazo() ? "Dentro do prazo" : "Fora do prazo", r.getGrupo(), r.getSolicitacao(),
        r.getDescricao(), r.getResponsavel(), r.getInicio() == null ? "" : f.format(r.getInicio()), r.getFim() == null ? "" : f.format(r.getFim()), r.getExtra() };
      for (int k = 0; k < c.length; k++) {
        String v = c[k] == null ? "" : c[k];
        if (v.matches("^[=+\\-@].*")) v = "'" + v;                // evita fórmula ao abrir no Excel
        sb.append('"').append(v.replace("\"", "\"\"")).append('"').append(k < c.length - 1 ? ";" : "\r\n");
      }
    }
    String nome = area.name().toLowerCase() + "-" + ano + "-" + String.format("%02d", mes) + ".csv";
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
      .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping("/importar")
  public String importarForm(Model model){ model.addAttribute("vazio", imps.count() == 0); return "importar"; }

  @PostMapping("/importar")
  public String importar(@RequestParam("arquivos") List<MultipartFile> arquivos, @RequestParam(defaultValue = "") String periodo, Model model){
    Integer ano = null, mes = null;
    if (periodo.matches("\\d{4}-\\d{2}")) { ano = Integer.parseInt(periodo.substring(0, 4)); mes = Integer.parseInt(periodo.substring(5)); }
    List<ImportacaoService.Retorno> rets = new ArrayList<>();
    for (MultipartFile f : arquivos) {
      if (f.isEmpty()) continue;
      ImportacaoService.Retorno r;
      try { r = service.importar(f::getInputStream, f.getOriginalFilename(), ano, mes); }
      catch (IllegalArgumentException e) { r = new ImportacaoService.Retorno(); r.arquivo = f.getOriginalFilename(); r.erro = e.getMessage(); }
      catch (Exception e) {
        log.error("Falha ao importar " + f.getOriginalFilename(), e);
        r = new ImportacaoService.Retorno(); r.arquivo = f.getOriginalFilename();
        r.erro = "Não foi possível ler o arquivo (" + e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "") + "). Ele é um .xlsx válido? Detalhes na janela preta do programa.";
      }
      rets.add(r);
    }
    if (rets.isEmpty()) { model.addAttribute("erro", "Escolha pelo menos um arquivo .xlsx."); model.addAttribute("vazio", imps.count() == 0); return "importar"; }
    model.addAttribute("rets", rets);
    rets.stream().filter(r -> r.importacao != null).findFirst().ifPresent(r -> { model.addAttribute("ano", r.importacao.getAno()); model.addAttribute("mes", r.importacao.getMes()); });
    return "importado";
  }

  @GetMapping("/importacoes")
  public String importacoes(Model model){ model.addAttribute("imps", imps.findAllByOrderByAnoDescMesDescAreaAsc()); return "importacoes"; }

  @PostMapping("/importacoes/{id}/excluir")
  public String excluir(@PathVariable Long id, RedirectAttributes ra){ service.excluir(id); ra.addFlashAttribute("ok", "Importação excluída."); return "redirect:/importacoes"; }
}
