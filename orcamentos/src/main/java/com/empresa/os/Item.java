package com.empresa.os;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
@Embeddable
public class Item {
  private String descricao; private Integer quantidade = 1; private BigDecimal valorUnitario = BigDecimal.ZERO;
  public String getDescricao(){return descricao;} public void setDescricao(String d){descricao=d;}
  public Integer getQuantidade(){return quantidade;} public void setQuantidade(Integer q){quantidade=q;}
  public BigDecimal getValorUnitario(){return valorUnitario;} public void setValorUnitario(BigDecimal v){valorUnitario=v;}
  public BigDecimal subtotal(){ return (valorUnitario==null?BigDecimal.ZERO:valorUnitario).multiply(BigDecimal.valueOf(quantidade==null?1:quantidade)); }
}
