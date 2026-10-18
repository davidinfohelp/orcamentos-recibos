package com.empresa.os;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
@Entity
public class Documento {
  public enum Tipo { ORCAMENTO, RECIBO }
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Enumerated(EnumType.STRING) private Tipo tipo = Tipo.ORCAMENTO;
  private Integer numero; private String cliente; private String telefone; private String cpfCnpj; private String dono;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate data = LocalDate.now();
  private String status = "Pendente"; private String formaPagamento;
  @Column(length = 2000) private String observacoes;
  @ElementCollection(fetch = FetchType.EAGER) private List<Item> itens = new ArrayList<>();
  public BigDecimal total(){ return itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add); }
  public Long getId(){return id;} public void setId(Long i){id=i;}
  public Tipo getTipo(){return tipo;} public void setTipo(Tipo t){tipo=t;}
  public Integer getNumero(){return numero;} public void setNumero(Integer n){numero=n;}
  public String getCliente(){return cliente;} public void setCliente(String c){cliente=c;}
  public String getTelefone(){return telefone;} public void setTelefone(String t){telefone=t;}
  public String getCpfCnpj(){return cpfCnpj;} public void setCpfCnpj(String c){cpfCnpj=c;}
  public String getDono(){return dono;} public void setDono(String d){dono=d;}
  public LocalDate getData(){return data;} public void setData(LocalDate d){data=d;}
  public String getStatus(){return status;} public void setStatus(String s){status=s;}
  public String getFormaPagamento(){return formaPagamento;} public void setFormaPagamento(String f){formaPagamento=f;}
  public String getObservacoes(){return observacoes;} public void setObservacoes(String o){observacoes=o;}
  public List<Item> getItens(){return itens;} public void setItens(List<Item> i){itens=i;}
}
