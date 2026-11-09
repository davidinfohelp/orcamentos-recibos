package com.empresa.os;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
  List<Cliente> findByDonoOrderByNomeAsc(String dono);
  Optional<Cliente> findByIdAndDono(Long id, String dono);
  Optional<Cliente> findFirstByDonoAndDocumento(String dono, String documento);
  Optional<Cliente> findFirstByDonoAndNomeIgnoreCase(String dono, String nome);
  @Query("select c from Cliente c where c.dono = :dono and (lower(c.nome) like :q or lower(c.nomeFantasia) like :q"
       + " or (:dig <> '' and c.documento like :digLike)) order by c.nome")
  List<Cliente> buscar(@Param("dono") String dono, @Param("q") String q, @Param("dig") String dig, @Param("digLike") String digLike);
  default List<Cliente> buscar(String dono, String texto){
    String t = texto == null ? "" : texto.trim().toLowerCase();
    String d = Cliente.digitos(t);
    return buscar(dono, "%" + t + "%", d, "%" + d + "%");
  }
}
