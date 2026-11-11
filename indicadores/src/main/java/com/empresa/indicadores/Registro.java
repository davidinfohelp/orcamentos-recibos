package com.empresa.indicadores;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Uma solicitação (linha da planilha) já classificada. */
@Entity @Table(name = "registros", indexes = @Index(name = "ix_registros_imp", columnList = "importacao_id"))
public class Registro {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) private Importacao importacao;
  /** "Inclusão" ou "Manutenção". */
  private String classificacao;
  /** Faixa de prazo como veio na planilha (Mesmo dia, 24 horas úteis, Dentro do Prazo...). */
  private String faixa;
  private boolean dentroPrazo;
  /** Processo (cliente), tarefa (fornecedor) ou tipo de material. */
  private String grupo;
  private String solicitacao;
  @Column(length = 300) private String descricao;
  private String responsavel;
  private LocalDateTime inicio; private LocalDateTime fim;
  private String extra;

  public Long getId(){return id;}
  public Importacao getImportacao(){return importacao;} public void setImportacao(Importacao i){importacao=i;}
  public String getClassificacao(){return classificacao;} public void setClassificacao(String c){classificacao=c;}
  public String getFaixa(){return faixa;} public void setFaixa(String f){faixa=f;}
  public boolean isDentroPrazo(){return dentroPrazo;} public void setDentroPrazo(boolean d){dentroPrazo=d;}
  public String getGrupo(){return grupo;} public void setGrupo(String g){grupo=g;}
  public String getSolicitacao(){return solicitacao;} public void setSolicitacao(String s){solicitacao=s;}
  public String getDescricao(){return descricao;} public void setDescricao(String d){descricao = d != null && d.length() > 300 ? d.substring(0, 300) : d;}
  public String getResponsavel(){return responsavel;} public void setResponsavel(String r){responsavel=r;}
  public LocalDateTime getInicio(){return inicio;} public void setInicio(LocalDateTime i){inicio=i;}
  public LocalDateTime getFim(){return fim;} public void setFim(LocalDateTime f){fim=f;}
  public String getExtra(){return extra;} public void setExtra(String e){extra=e;}
}
