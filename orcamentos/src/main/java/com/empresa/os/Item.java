package com.empresa.os;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
@Embeddable
public class Item {
  private String descricao; private Integer quantidade = 1; private BigDecimal valorUnitario = BigDecimal.ZERO;
  /** "SERVICO" ou "ITEM" (produto vendido). Registros antigos ficam nulos e contam como serviço. */
  private String tipo = "SERVICO";
  public String getDescricao(){return descricao;} public void setDescricao(String d){descricao=d;}
  public Integer getQuantidade(){return quantidade;} public void setQuantidade(Integer q){quantidade=q;}
  public BigDecimal getValorUnitario(){return valorUnitario;} public void setValorUnitario(BigDecimal v){valorUnitario=v;}
  public String getTipo(){return tipo;} public void setTipo(String t){tipo=t;}
  public boolean ehItem(){ return "ITEM".equals(tipo); }
  public String rotulo(){ return ehItem() ? "Item" : "Serviço"; }
  public BigDecimal subtotal(){ return (valorUnitario==null?BigDecimal.ZERO:valorUnitario).multiply(BigDecimal.valueOf(quantidade==null?1:quantidade)); }
}
