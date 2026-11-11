package com.empresa.indicadores;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

interface ImportacaoRepository extends JpaRepository<Importacao, Long> {
  Optional<Importacao> findByAreaAndAnoAndMes(Area area, int ano, int mes);
  List<Importacao> findAllByOrderByAnoDescMesDescAreaAsc();
  List<Importacao> findByAnoAndMes(int ano, int mes);
}

interface RegistroRepository extends JpaRepository<Registro, Long> {
  List<Registro> findByImportacaoOrderByIdAsc(Importacao i);
  /** [importacaoId, classificacao, dentroPrazo, quantidade] — base do painel e do histórico. */
  @Query("select r.importacao.id, r.classificacao, r.dentroPrazo, count(r) from Registro r group by r.importacao.id, r.classificacao, r.dentroPrazo")
  List<Object[]> contagens();
  @Modifying @Query("delete from Registro r where r.importacao = :i")
  void apagarDa(@Param("i") Importacao i);
}
