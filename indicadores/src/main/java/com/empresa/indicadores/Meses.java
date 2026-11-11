package com.empresa.indicadores;
public final class Meses {
  static final String[] NOMES = {"janeiro","fevereiro","março","abril","maio","junho","julho","agosto","setembro","outubro","novembro","dezembro"};
  private Meses(){}
  public static String nome(int m){ return m >= 1 && m <= 12 ? NOMES[m - 1].substring(0, 1).toUpperCase() + NOMES[m - 1].substring(1) : "?"; }
}
