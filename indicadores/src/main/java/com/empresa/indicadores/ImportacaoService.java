package com.empresa.indicadores;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ImportacaoService {
  private final LeitorPlanilha leitor; private final ImportacaoRepository imps; private final RegistroRepository regs;
  public ImportacaoService(LeitorPlanilha l, ImportacaoRepository i, RegistroRepository r){ leitor = l; imps = i; regs = r; }

  /** Resumo do que aconteceu com cada arquivo, para mostrar na tela. */
  public static class Retorno {
    public String arquivo, erro, aviso, origemMes; public Area area; public Importacao importacao; public boolean substituiu;
    public String getArquivo(){return arquivo;} public String getErro(){return erro;} public String getAviso(){return aviso;}
    public String getOrigemMes(){return origemMes;} public Area getArea(){return area;} public Importacao getImportacao(){return importacao;}
    public boolean isSubstituiu(){return substituiu;}
  }

  /**
   * Importa um arquivo. Se ano/mes vierem preenchidos, eles mandam; senão usa o mês detectado na planilha.
   * Se já existir importação da mesma área no mesmo mês, ela é substituída.
   */
  @Transactional
  public Retorno importar(LeitorPlanilha.Fonte fonte, String arquivo, Integer ano, Integer mes) throws Exception {
    Retorno ret = new Retorno(); ret.arquivo = arquivo;
    LeitorPlanilha.Resultado res = leitor.ler(fonte, arquivo, mes);
    ret.area = res.area;
    int a = ano != null ? ano : res.ano != null ? res.ano : java.time.LocalDate.now().getYear();
    int m = mes != null ? mes : res.mes != null ? res.mes : 0;
    if (m < 1 || m > 12) throw new IllegalArgumentException("Não consegui descobrir o mês desta planilha. Escolha o mês no campo \"Mês de referência\" e importe de novo.");
    if (mes != null && res.mes != null && (!res.mes.equals(mes) || (res.ano != null && !res.ano.equals(ano))))
      ret.aviso = "A planilha parece ser de " + Meses.nome(res.mes) + (res.ano != null ? "/" + res.ano : "") + ", mas foi importada como " + Meses.nome(m) + "/" + a + ", como você escolheu.";
    ret.origemMes = mes != null ? "escolhido por você" : res.origemMes;
    if (res.registros.isEmpty()) throw new IllegalArgumentException("A planilha de " + res.area.getNome() + " foi reconhecida, mas não tem nenhuma linha com indicador preenchido.");

    Optional<Importacao> antiga = imps.findByAreaAndAnoAndMes(res.area, a, m);
    if (antiga.isPresent()) { regs.apagarDa(antiga.get()); imps.delete(antiga.get()); imps.flush(); ret.substituiu = true; }

    Importacao imp = new Importacao();
    imp.setArea(res.area); imp.setAno(a); imp.setMes(m); imp.setArquivo(arquivo); imp.setAba(res.aba);
    imp.setLinhas(res.registros.size()); imp.setIgnoradas(res.ignoradas); imp.setOutrosMeses(res.outrosMeses);
    imps.save(imp);
    for (Registro r : res.registros) r.setImportacao(imp);
    regs.saveAll(res.registros);
    ret.importacao = imp;
    return ret;
  }

  @Transactional
  public void excluir(Long id){
    imps.findById(id).ifPresent(i -> { regs.apagarDa(i); imps.delete(i); });
  }
}
