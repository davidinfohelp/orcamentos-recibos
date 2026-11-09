package com.empresa.os;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * Consulta dados públicos de um CNPJ. Tenta a BrasilAPI e, se falhar, a CNPJ.ws (pública).
 * A consulta é feita pelo servidor, então o navegador do usuário não fala direto com essas APIs.
 */
@Service
public class CnpjService {
  private static final Logger log = LoggerFactory.getLogger(CnpjService.class);
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6))
      .followRedirects(HttpClient.Redirect.NORMAL).proxy(java.net.ProxySelector.getDefault()).build();
  private final ObjectMapper json = new ObjectMapper();

  /** Retorna os dados prontos para preencher o cadastro, ou vazio se o CNPJ não foi encontrado. */
  public Optional<Map<String, String>> consultar(String cnpj){
    String d = Cliente.digitos(cnpj);
    if (!cnpjValido(d)) throw new IllegalArgumentException("CNPJ inválido.");
    try { JsonNode n = get("https://brasilapi.com.br/api/cnpj/v1/" + d); if (n != null) return Optional.of(deBrasilApi(d, n)); }
    catch (Exception e) { log.warn("BrasilAPI falhou para {}: {}", d, e.toString()); }
    try { JsonNode n = get("https://publica.cnpj.ws/cnpj/" + d); if (n != null) return Optional.of(deCnpjWs(d, n)); }
    catch (Exception e) { log.warn("CNPJ.ws falhou para {}: {}", d, e.toString()); throw new IllegalStateException("Serviço de consulta indisponível. Tente de novo em instantes."); }
    return Optional.empty();
  }

  /** null = não encontrado (404); exceção = erro de rede/servidor. */
  private JsonNode get(String url) throws Exception {
    HttpRequest r = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
        .header("Accept", "application/json").header("User-Agent", "orcamentos-recibos/1.0").GET().build();
    HttpResponse<String> resp = http.send(r, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() == 404 || resp.statusCode() == 400) return null;
    if (resp.statusCode() != 200) throw new IllegalStateException("HTTP " + resp.statusCode());
    return json.readTree(resp.body());
  }

  private static String t(JsonNode n, String campo){
    JsonNode v = n == null ? null : n.get(campo);
    if (v == null || v.isNull()) return "";
    String s = v.asText().trim();
    return s.equalsIgnoreCase("null") ? "" : s;
  }
  private static String junta(String sep, String... partes){
    StringJoiner j = new StringJoiner(sep);
    for (String p : partes) if (p != null && !p.isBlank()) j.add(p.trim());
    return j.toString();
  }
  private static String tel(String ddd, String num){
    String a = Cliente.digitos(ddd), b = Cliente.digitos(num);
    if (b.isEmpty()) return "";
    if (a.isEmpty() && b.length() >= 10) { a = b.substring(0, 2); b = b.substring(2); }
    String nf = b.length() == 9 ? b.substring(0, 5) + "-" + b.substring(5) : b.length() == 8 ? b.substring(0, 4) + "-" + b.substring(4) : b;
    return a.isEmpty() ? nf : "(" + a + ") " + nf;
  }

  private static Map<String, String> monta(String cnpj, String nome, String fantasia, String tel, String email, String end, String situacao){
    Map<String, String> m = new LinkedHashMap<>();
    m.put("cpfCnpj", Cliente.formata(cnpj)); m.put("nome", nome); m.put("nomeFantasia", fantasia);
    m.put("telefone", tel); m.put("email", email == null ? "" : email.toLowerCase()); m.put("endereco", end); m.put("situacao", situacao);
    return m;
  }

  private Map<String, String> deBrasilApi(String cnpj, JsonNode n){
    String ddd = t(n, "ddd_telefone_1");
    String end = junta(", ", junta(" ", t(n, "descricao_tipo_de_logradouro"), t(n, "logradouro")), t(n, "numero"), t(n, "complemento"), t(n, "bairro"),
        junta("/", t(n, "municipio"), t(n, "uf")), t(n, "cep").isEmpty() ? "" : "CEP " + t(n, "cep").replaceFirst("(\\d{5})(\\d{3})", "$1-$2"));
    return monta(cnpj, t(n, "razao_social"), t(n, "nome_fantasia"), tel("", ddd), t(n, "email"), end, t(n, "descricao_situacao_cadastral"));
  }

  private Map<String, String> deCnpjWs(String cnpj, JsonNode n){
    JsonNode e = n.get("estabelecimento");
    JsonNode cid = e == null ? null : e.get("cidade"), est = e == null ? null : e.get("estado");
    String cep = t(e, "cep");
    String end = junta(", ", junta(" ", t(e, "tipo_logradouro"), t(e, "logradouro")), t(e, "numero"), t(e, "complemento"), t(e, "bairro"),
        junta("/", t(cid, "nome"), t(est, "sigla")), cep.isEmpty() ? "" : "CEP " + cep.replaceFirst("(\\d{5})(\\d{3})", "$1-$2"));
    return monta(cnpj, t(n, "razao_social"), t(e, "nome_fantasia"), tel(t(e, "ddd1"), t(e, "telefone1")), t(e, "email"), end, t(e, "situacao_cadastral"));
  }

  /** Confere os dígitos verificadores do CNPJ. */
  public static boolean cnpjValido(String d){
    if (d == null || !d.matches("\\d{14}") || d.chars().distinct().count() == 1) return false;
    int[] p1 = {5,4,3,2,9,8,7,6,5,4,3,2}, p2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};
    int s = 0; for (int i = 0; i < 12; i++) s += (d.charAt(i) - '0') * p1[i];
    int dv1 = s % 11 < 2 ? 0 : 11 - s % 11;
    s = 0; for (int i = 0; i < 13; i++) s += (d.charAt(i) - '0') * p2[i];
    int dv2 = s % 11 < 2 ? 0 : 11 - s % 11;
    return dv1 == d.charAt(12) - '0' && dv2 == d.charAt(13) - '0';
  }
}
