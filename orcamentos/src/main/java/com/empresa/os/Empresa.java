package com.empresa.os;
import jakarta.persistence.*;

/** Dados da empresa que emite os documentos. Cada usuário (dono) tem a sua, isolada das demais. */
@Entity @Table(name = "empresas")
public class Empresa {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(unique = true, nullable = false) private String dono;
  private String nome; private String cpfCnpj; private String telefone; private String email; private String endereco;
  private String cor = "#1f6feb";
  @Column(length = 400000) private String logo;
  public Long getId(){return id;}
  public String getDono(){return dono;} public void setDono(String d){dono=d;}
  public String getNome(){return nome;} public void setNome(String n){nome=n;}
  public String getCpfCnpj(){return cpfCnpj;} public void setCpfCnpj(String c){cpfCnpj=c;}
  public String getTelefone(){return telefone;} public void setTelefone(String t){telefone=t;}
  public String getEmail(){return email;} public void setEmail(String e){email=e;}
  public String getEndereco(){return endereco;} public void setEndereco(String e){endereco=e;}
  public String getCor(){return cor;} public void setCor(String c){cor=c;}
  public String getLogo(){return logo;} public void setLogo(String l){logo=l;}
}
