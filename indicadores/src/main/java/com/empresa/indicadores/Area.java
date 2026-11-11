package com.empresa.indicadores;
public enum Area {
  FORNECEDOR("Fornecedor", "Tarefa"), CLIENTE("Cliente", "Processo"), MATERIAL("Material", "Tipo de material");
  private final String nome, grupo;
  Area(String n, String g){ nome = n; grupo = g; }
  public String getNome(){ return nome; }
  /** Nome do agrupamento usado no detalhamento. */
  public String getGrupo(){ return grupo; }
}
