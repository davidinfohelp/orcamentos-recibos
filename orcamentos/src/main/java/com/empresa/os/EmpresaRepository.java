package com.empresa.os;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
  Optional<Empresa> findByDono(String dono);
}
