package com.empresa.os;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
  List<Documento> findByDonoOrderByIdDesc(String dono);
  Optional<Documento> findByIdAndDono(Long id, String dono);
  @Query("select coalesce(max(d.numero),0) from Documento d where d.tipo = :t and d.dono = :dono")
  int maxNumero(@Param("t") Documento.Tipo t, @Param("dono") String dono);
}
