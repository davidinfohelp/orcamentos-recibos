package com.empresa.os;
import jakarta.persistence.*;
@Entity @Table(name = "usuarios")
public class Usuario {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(unique = true, nullable = false) private String username;
  @Column(nullable = false) private String password;
  public Long getId(){return id;}
  public String getUsername(){return username;} public void setUsername(String u){username=u;}
  public String getPassword(){return password;} public void setPassword(String p){password=p;}
}
