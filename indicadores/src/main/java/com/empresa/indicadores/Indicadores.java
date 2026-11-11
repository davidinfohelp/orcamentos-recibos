package com.empresa.indicadores;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.*;

/** Cálculos mostrados na tela: indicador por classificação, por faixa e por grupo. */
@Service
public class Indicadores {
  private final ImportacaoRepository imps; private final RegistroRepository regs;
  public Indicadores(ImportacaoRepository i, RegistroRepository r){ imps = i; regs = r; }

  public static class Linha {
    private final String nome; private final String classificacao; long total, dentro;
    final Map<String, Long> faixas = new LinkedHashMap<>();
    Linha(String nome, String classificacao){ this.nome = nome; this.classificacao = classificacao; }
    public String getNome(){return nome;} public String getClassificacao(){return classificacao;}
    public long getTotal(){return total;} public long getDentro(){return dentro;} public long getFora(){return total - dentro;}
    public double getPct(){ return total == 0 ? 0 : dentro * 100.0 / total; }
    public Map<String, Long> getFaixas(){return faixas;}
  }

  static int ordemClass(String c){ return "Inclusão".equals(c) ? 0 : "Manutenção".equals(c) ? 1 : 2; }
  static int ordemFaixa(String f){
    String n = LeitorPlanilha.norm(f);
    if (n.startsWith("mesmo dia") || n.startsWith("dentro")) return 0;
    if (n.startsWith("mais de") || n.startsWith("fora")) return 900;
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)").matcher(n);
    return m.find() ? Integer.parseInt(m.group(1)) : 500;
  }

  /** Uma linha por classificação (Inclusão, Manutenção) com as faixas de prazo. */
  public List<Linha> porClassificacao(List<Registro> rs){
    Map<String, Linha> m = new TreeMap<>(Comparator.comparingInt(Indicadores::ordemClass).thenComparing(Comparator.naturalOrder()));
    for (Registro r : rs) {
      Linha l = m.computeIfAbsent(r.getClassificacao(), k -> new Linha(k, k));
      l.total++; if (r.isDentroPrazo()) l.dentro++;
    }
    List<String> faixas = faixas(rs);
    for (Linha l : m.values()) for (String f : faixas) l.faixas.put(f, 0L);
    for (Registro r : rs) m.get(r.getClassificacao()).faixas.merge(r.getFaixa(), 1L, Long::sum);
    return new ArrayList<>(m.values());
  }

  public List<String> faixas(List<Registro> rs){
    return rs.stream().map(Registro::getFaixa).distinct()
      .sorted(Comparator.comparingInt(Indicadores::ordemFaixa).thenComparing(Comparator.naturalOrder())).toList();
  }

  /** Detalhe por grupo (processo / tarefa / tipo de material) dentro de cada classificação. */
  public List<Linha> porGrupo(List<Registro> rs){
    Map<String, Linha> m = new LinkedHashMap<>();
    for (Registro r : rs) {
      Linha l = m.computeIfAbsent(r.getClassificacao() + "\u0000" + r.getGrupo(), k -> new Linha(r.getGrupo(), r.getClassificacao()));
      l.total++; if (r.isDentroPrazo()) l.dentro++;
    }
    return m.values().stream().sorted(Comparator.comparingInt((Linha l) -> ordemClass(l.getClassificacao()))
      .thenComparing(Comparator.comparingLong(Linha::getTotal).reversed())).collect(Collectors.toList());
  }

  /** Para o painel e o histórico: importacaoId -> classificação -> Linha (sem faixas). */
  public Map<Long, Map<String, Linha>> resumoDeTodas(){
    Map<Long, Map<String, Linha>> out = new HashMap<>();
    for (Object[] o : regs.contagens()) {
      Long id = (Long) o[0]; String c = (String) o[1]; boolean d = (Boolean) o[2]; long q = ((Number) o[3]).longValue();
      Linha l = out.computeIfAbsent(id, k -> new TreeMap<>(Comparator.comparingInt(Indicadores::ordemClass).thenComparing(Comparator.naturalOrder())))
        .computeIfAbsent(c, k -> new Linha(k, k));
      l.total += q; if (d) l.dentro += q;
    }
    return out;
  }

  /** Períodos importados, do mais recente para o mais antigo, como [ano, mes]. */
  public List<int[]> periodos(){
    LinkedHashMap<String, int[]> p = new LinkedHashMap<>();
    for (Importacao i : imps.findAllByOrderByAnoDescMesDescAreaAsc()) p.putIfAbsent(i.getAno() + "-" + i.getMes(), new int[]{i.getAno(), i.getMes()});
    return new ArrayList<>(p.values());
  }
}
