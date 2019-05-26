/* ----------------------------------------------------------------------------
 * All rights reserved © 2018 Multiva.
 *  
 * This software contains information that is exclusive property of 
 * Multiva, this information is considered confidential.
 * It is strictly forbidden the copy or spreading of any part of this document 
 * in any format, whether mechanic or electronic.
 * ---------------------------------------------------------------------------
 * File name: EstatusRepository.java
 * Original Author: StratPlus
 * Creation Date: 17/10/2018
 * ---------------------------------------------------------------------------
 */
package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Estatus;

/**
 * <code>EstatusRepository</code>.
 *
 * @author ${eduardo.santiago}
 * @version 1.0
 */
public interface EstatusRepository extends JpaRepository<Estatus, Long> {

    /**
     * Find by id.
     *
     * @param estatus
     *            estatus
     * @return estatus
     */
    public Estatus findById(Integer id);
    public Estatus findByDescripcion(String descripcion);

}
