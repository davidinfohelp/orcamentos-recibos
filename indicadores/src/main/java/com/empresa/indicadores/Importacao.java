package com.empresa.indicadores;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Uma planilha importada: uma área (fornecedor/cliente/material) em um mês. */
@Entity @Table(name = "importacoes", uniqueConstraints = @UniqueConstraint(columnNames = {"area", "ano", "mes"}))
public class Importacao {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private Area area;
  private int ano; private int mes;
  private String arquivo; private String aba;
  private LocalDateTime importadoEm = LocalDateTime.now();
  private int linhas; private int ignoradas;

  public Long getId(){return id;}
  public Area getArea(){return area;} public void setArea(Area a){area=a;}
  public int getAno(){return ano;} public void setAno(int a){ano=a;}
  public int getMes(){return mes;} public void setMes(int m){mes=m;}
  public String getArquivo(){return arquivo;} public void setArquivo(String a){arquivo=a;}
  public String getAba(){return aba;} public void setAba(String a){aba=a;}
  public LocalDateTime getImportadoEm(){return importadoEm;}
  public int getLinhas(){return linhas;} public void setLinhas(int l){linhas=l;}
  public int getIgnoradas(){return ignoradas;} public void setIgnoradas(int i){ignoradas=i;}
  public String getPeriodo(){ return Meses.nome(mes) + "/" + ano; }
}
