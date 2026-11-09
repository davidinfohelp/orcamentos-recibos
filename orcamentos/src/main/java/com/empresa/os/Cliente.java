package com.empresa.os;
import jakarta.persistence.*;

/** Cliente da base de cada usuário (dono). Uma conta nunca enxerga os clientes de outra. */
@Entity @Table(name = "clientes", indexes = @Index(name = "ix_clientes_dono_doc", columnList = "dono,documento"))
public class Cliente {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false) private String dono;
  @Column(nullable = false) private String nome;
  private String nomeFantasia;
  /** CPF/CNPJ formatado para exibição. */
  private String cpfCnpj;
  /** Somente os dígitos do CPF/CNPJ, usado para busca e para evitar cadastro duplicado. */
  private String documento;
  private String telefone; private String email; private String endereco;
  private String situacao;
  @Column(length = 1000) private String observacoes;

  public static String digitos(String s){ return s == null ? "" : s.replaceAll("\\D", ""); }
  /** Formata 11 dígitos como CPF e 14 como CNPJ; outros tamanhos ficam como vieram. */
  public static String formata(String s){
    String d = digitos(s);
    if (d.length() == 11) return d.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    if (d.length() == 14) return d.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    return s == null || s.isBlank() ? null : s.trim();
  }

  public Long getId(){return id;} public void setId(Long i){id=i;}
  public String getDono(){return dono;} public void setDono(String d){dono=d;}
  public String getNome(){return nome;} public void setNome(String n){nome=n;}
  public String getNomeFantasia(){return nomeFantasia;} public void setNomeFantasia(String n){nomeFantasia=n;}
  public String getCpfCnpj(){return cpfCnpj;}
  public void setCpfCnpj(String c){ cpfCnpj = formata(c); String d = digitos(c); documento = d.isEmpty() ? null : d; }
  public String getDocumento(){return documento;}
  public String getTelefone(){return telefone;} public void setTelefone(String t){telefone=t;}
  public String getEmail(){return email;} public void setEmail(String e){email=e;}
  public String getEndereco(){return endereco;} public void setEndereco(String e){endereco=e;}
  public String getSituacao(){return situacao;} public void setSituacao(String s){situacao=s;}
  public String getObservacoes(){return observacoes;} public void setObservacoes(String o){observacoes=o;}
}
