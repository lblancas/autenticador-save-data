package com.example.demo.repository.reglas;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.catalogos.TipoUsuario;
import com.example.demo.domain.reglas.Cuenta;
import com.example.demo.domain.reglas.Persona;
public interface CuentaRepository extends JpaRepository<Cuenta, Long> 
{
	Optional<Cuenta> findById(Long id);
	List<Cuenta> findAll();
	List<Cuenta> findByIdpersona(Persona id);
	
}
