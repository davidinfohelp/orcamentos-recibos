package com.empresa.indicadores;
import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

/**
 * Lê um .xlsx, descobre se é a planilha de FORNECEDOR, CLIENTE ou MATERIAL pelas colunas
 * (não pelo nome do arquivo) e transforma cada linha em um {@link Registro}.
 * Lê em streaming para aguentar planilhas grandes com pouca memória.
 */
@Component
public class LeitorPlanilha {

  /** Resultado da leitura de um arquivo. */
  public static class Resultado {
    public Area area; public String aba;
    public Integer ano, mes;            // mês/ano detectados
    public String origemMes;            // como o mês foi detectado
    public final List<Registro> registros = new ArrayList<>();
    public int ignoradas;
  }

  /** Data lida + se veio de uma célula de data do Excel (essas às vezes vêm com dia e mês trocados). */
  private record Data(LocalDateTime v, boolean celula){ }
  private static class Tmp { Registro r; Data ini, fim; Integer mesColuna; }

  // ---------------------------------------------------------------- leitura

  public Resultado ler(InputStream in, String nomeArquivo) throws Exception {
    try (Workbook wb = StreamingReader.builder().rowCacheSize(500).bufferSize(16384).open(in)) {
      for (Sheet sheet : wb) {
        Iterator<Row> it = sheet.iterator();
        List<String> cab = null; int tentativas = 0;
        while (it.hasNext() && tentativas++ < 10) {           // cabeçalho = 1ª linha não vazia
          List<String> l = textos(it.next());
          if (l.stream().anyMatch(s -> !s.isEmpty())) { cab = l; break; }
        }
        if (cab == null) continue;
        Area area = identificar(cab);
        if (area == null) continue;
        Resultado res = new Resultado(); res.area = area; res.aba = sheet.getSheetName();
        List<Tmp> tmps = lerLinhas(area, cab, it, res);
        detectarMes(res, tmps, nomeArquivo);
        return res;
      }
    }
    throw new IllegalArgumentException("Não reconheci esta planilha como Fornecedor, Cliente ou Material. "
      + "Confira se ela tem a aba RESUMO (fornecedor/cliente) ou Lista (material) com o cabeçalho original.");
  }

  /** Decide a área pela "assinatura" das colunas. */
  static Area identificar(List<String> cab){
    Set<String> c = new HashSet<>(); for (String s : cab) c.add(norm(s));
    long indicadores = cab.stream().filter(s -> norm(s).equals("indicador")).count();
    if (c.contains("tipo material") && c.contains("indicador") && c.contains("classificacao") && (c.contains("idsin") || c.contains("workflow")))
      return Area.MATERIAL;
    if (c.contains("prazo") && c.stream().anyMatch(s -> s.endsWith("classificacao")) && c.stream().anyMatch(s -> s.startsWith("supplier")))
      return Area.FORNECEDOR;
    if (c.contains("cod. do cliente") && c.contains("processo") && indicadores >= 2)
      return Area.CLIENTE;
    return null;
  }

  private List<Tmp> lerLinhas(Area area, List<String> cab, Iterator<Row> it, Resultado res){
    Col col = new Col(cab);
    // índices das colunas de cada área
    int iClass, iFaixa, iSol, iIni, iFim, iGrupo, iResp, iDesc = -1, iDesc2 = -1, iDesc3 = -1, iExtra = -1, iExtra2 = -1, iMes = -1;
    switch (area) {
      case FORNECEDOR -> {
        iClass = col.terminaCom("classificacao"); iFaixa = col.igual("prazo"); iSol = col.igual("supplier -> id");
        iIni = col.terminaCom("data inicio"); iFim = col.terminaCom("data fim"); iGrupo = col.igual("tarefa -> nome");
        iResp = col.igual("tarefa -> completado por"); iExtra = col.igual("motivo do atraso");
      }
      case CLIENTE -> {
        iClass = col.n("indicador", 1); iFaixa = col.n("indicador", 2); iSol = col.igual("solicitacao");
        iIni = col.igual("data da criacao"); iFim = col.igual("data da finalizacao"); iGrupo = col.igual("processo");
        iResp = col.igual("vendedor"); iDesc = col.igual("razao social"); iDesc2 = col.igual("nome"); iDesc3 = col.igual("sobrenome");
        iExtra = col.igual("situacao");
      }
      default -> {
        iClass = col.igual("classificacao"); iFaixa = col.igual("indicador"); iSol = col.igual("idsin");
        iIni = col.igual("data solicitacao"); iFim = col.igual("data finalizacao"); iGrupo = col.igual("tipo material");
        iResp = col.igual("solicitante"); iDesc = col.igual("workflow"); iExtra = col.igual("dias totais"); iExtra2 = col.igual("sla");
        iMes = col.igual("mes");
      }
    }
    List<Tmp> tmps = new ArrayList<>();
    while (it.hasNext()) {
      Row row = it.next();
      Function<Integer, String> t = i -> i < 0 ? "" : texto(row.getCell(i));
      String cls = t.apply(iClass), fx = t.apply(iFaixa), sol = t.apply(iSol);
      if (cls.isEmpty() && fx.isEmpty()) { if (!sol.isEmpty()) res.ignoradas++; continue; }   // linha sem indicador
      if (cls.isEmpty() || fx.isEmpty()) { res.ignoradas++; continue; }
      Registro r = new Registro();
      r.setClassificacao(classificacao(cls));
      r.setFaixa(faixa(fx));
      r.setDentroPrazo(dentro(r.getFaixa()));
      String grupo = t.apply(iGrupo);
      r.setGrupo(grupo.isEmpty() ? "(sem informação)" : area == Area.MATERIAL ? grupo.toUpperCase() : grupo);
      r.setSolicitacao(sol);
      r.setResponsavel(t.apply(iResp));
      if (area == Area.CLIENTE) {
        String razao = t.apply(iDesc);
        r.setDescricao(!razao.isEmpty() ? razao : (t.apply(iDesc2) + " " + t.apply(iDesc3)).trim());
      } else if (iDesc >= 0) r.setDescricao(t.apply(iDesc));
      if (area == Area.MATERIAL) {
        String dias = t.apply(iExtra), sla = t.apply(iExtra2);
        r.setExtra(dias.isEmpty() ? "" : dias + " de " + sla + " dias");
      } else r.setExtra(t.apply(iExtra));
      Tmp x = new Tmp(); x.r = r;
      x.ini = data(iIni < 0 ? null : row.getCell(iIni)); x.fim = data(iFim < 0 ? null : row.getCell(iFim));
      if (iMes >= 0) { try { x.mesColuna = (int) Double.parseDouble(t.apply(iMes)); } catch (NumberFormatException ignored) { } }
      tmps.add(x);
    }
    for (Tmp x : tmps) res.registros.add(x.r);
    return tmps;
  }

  // ---------------------------------------------------------------- mês de referência

  private static final Pattern MES_NUM = Pattern.compile("(?:^|[^0-9])(0?[1-9]|1[0-2])[._ -]");

  private void detectarMes(Resultado res, List<Tmp> tmps, String arquivo){
    // 1) nome do mês no arquivo ("OUTUBRO") ou número no início ("10. Outubro")
    String a = norm(arquivo == null ? "" : arquivo.replaceFirst("^[0-9a-f]{8}-", ""));
    for (int m = 1; m <= 12; m++) if (a.contains(norm(Meses.NOMES[m - 1]))) { res.mes = m; res.origemMes = "nome do arquivo"; break; }
    // 2) coluna MÊS (material)
    if (res.mes == null) {
      Integer m = moda(tmps.stream().map(x -> x.mesColuna).filter(Objects::nonNull).toList());
      if (m != null) { res.mes = m; res.origemMes = "coluna MÊS"; }
    }
    // 3) datas de finalização escritas como texto (dd/mm/aaaa, sem ambiguidade)
    if (res.mes == null) {
      Integer m = moda(tmps.stream().filter(x -> x.fim != null && !x.fim.celula()).map(x -> x.fim.v().getMonthValue()).toList());
      if (m != null) { res.mes = m; res.origemMes = "datas de finalização"; }
    }
    // 4) datas de finalização em células de data
    if (res.mes == null) {
      Integer m = moda(tmps.stream().filter(x -> x.fim != null).map(x -> x.fim.v().getMonthValue()).toList());
      if (m != null) { res.mes = m; res.origemMes = "datas de finalização"; }
    }
    if (res.mes == null) {
      Matcher mm = MES_NUM.matcher(a);
      if (mm.find()) { res.mes = Integer.parseInt(mm.group(1)); res.origemMes = "número no nome do arquivo"; }
    }
    // corrige datas com dia/mês trocados e define o ano
    List<Integer> anos = new ArrayList<>();
    for (Tmp x : tmps) {
      LocalDateTime fim = x.fim == null ? null : corrigirFim(x.fim, res.mes);
      LocalDateTime ini = x.ini == null ? null : corrigirIni(x.ini, fim);
      // fim antes do início: se trocar dia/mês do fim resolver, troca (ex.: 11/03 que na verdade é 03/11)
      if (fim != null && ini != null && fim.isBefore(ini) && x.fim.celula()) {
        LocalDateTime t = trocar(fim);
        if (t != null && !t.isBefore(ini)) fim = t;
      }
      x.r.setFim(fim); x.r.setInicio(ini);
      if (fim != null && (res.mes == null || fim.getMonthValue() == res.mes)) anos.add(fim.getYear());
    }
    res.ano = moda(anos);
  }

  private static LocalDateTime corrigirFim(Data d, Integer mes){
    LocalDateTime v = d.v();
    if (d.celula() && mes != null && v.getMonthValue() != mes && v.getDayOfMonth() == mes) return trocar(v);
    return v;
  }
  private static LocalDateTime corrigirIni(Data d, LocalDateTime fim){
    LocalDateTime v = d.v();
    if (!d.celula() || fim == null || v.getDayOfMonth() > 12) return v;
    LocalDateTime t = trocar(v);
    if (t == null) return v;
    boolean vOk = !v.isAfter(fim), tOk = !t.isAfter(fim);
    if (tOk && (!vOk || Duration.between(t, fim).compareTo(Duration.between(v, fim)) < 0)) return t;
    return v;
  }
  private static LocalDateTime trocar(LocalDateTime v){
    try { return v.withDayOfMonth(1).withMonth(v.getDayOfMonth()).withDayOfMonth(v.getMonthValue()); }
    catch (DateTimeException e) { return null; }
  }
  private static <T> T moda(List<T> l){
    Map<T, Integer> c = new HashMap<>(); for (T v : l) c.merge(v, 1, Integer::sum);
    return c.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
  }

  // ---------------------------------------------------------------- normalizações

  static String classificacao(String s){
    String n = norm(s);
    if (n.contains("manut")) return "Manutenção";
    if (n.contains("inclus") || n.contains("cadastro")) return "Inclusão";
    return s.trim();
  }
  static String faixa(String s){
    String t = s.trim().replaceAll("\\s+", " ");
    String n = norm(t);
    if (n.equals("mesmo dia")) return "Mesmo dia";
    if (n.equals("dentro do prazo")) return "Dentro do prazo";
    if (n.equals("fora do prazo")) return "Fora do prazo";
    return t.substring(0, 1).toUpperCase() + t.substring(1);
  }
  /** "Mais de 48/72 horas úteis" e "Fora do prazo" contam como fora do prazo; o resto, dentro. */
  static boolean dentro(String faixa){
    String n = norm(faixa);
    return !(n.startsWith("mais de") || n.startsWith("fora"));
  }

  static String norm(String s){
    if (s == null) return "";
    String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return n.toLowerCase(Locale.ROOT).replace(' ', ' ').replaceAll("\\s+", " ").trim();
  }

  // ---------------------------------------------------------------- células

  private static final DataFormatter FMT = new DataFormatter(new Locale("pt", "BR"));
  private static final DateTimeFormatter[] FORMATOS = {
    DateTimeFormatter.ofPattern("d/M/yyyy H:mm:ss"), DateTimeFormatter.ofPattern("d/M/yyyy H:mm"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'H:mm:ss")};

  private static List<String> textos(Row r){
    List<String> l = new ArrayList<>();
    for (int i = 0; i < r.getLastCellNum(); i++) l.add(texto(r.getCell(i)));
    return l;
  }

  static String texto(Cell c){
    if (c == null) return "";
    CellType t = c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
    switch (t) {
      case STRING: return c.getStringCellValue().trim();
      case NUMERIC: {
        if (DateUtil.isCellDateFormatted(c)) return FMT.formatCellValue(c);
        double d = c.getNumericCellValue();
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        return String.valueOf(d);
      }
      case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
      default: return "";
    }
  }

  private static Data data(Cell c){
    if (c == null) return null;
    CellType t = c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
    if (t == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) return new Data(c.getLocalDateTimeCellValue(), true);
    if (t != CellType.STRING) return null;
    String s = c.getStringCellValue().trim();
    if (s.isEmpty()) return null;
    for (DateTimeFormatter f : FORMATOS) { try { return new Data(LocalDateTime.parse(s, f), false); } catch (Exception ignored) { } }
    try { return new Data(LocalDate.parse(s, DateTimeFormatter.ofPattern("d/M/yyyy")).atStartOfDay(), false); } catch (Exception ignored) { }
    return null;
  }

  /** Localiza colunas pelo nome normalizado. */
  private static class Col {
    final List<String> n = new ArrayList<>();
    Col(List<String> cab){ for (String s : cab) n.add(norm(s)); }
    int igual(String nome){ return n.indexOf(nome); }
    int terminaCom(String fim){ for (int i = 0; i < n.size(); i++) if (n.get(i).endsWith(fim)) return i; return -1; }
    /** índice da k-ésima coluna com esse nome (1 = primeira). */
    int n(String nome, int k){ int c = 0; for (int i = 0; i < n.size(); i++) if (n.get(i).equals(nome) && ++c == k) return i; return -1; }
  }
}
