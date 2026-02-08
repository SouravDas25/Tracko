package com.trako.repositories;

import com.trako.entities.NlpData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NlpDataRepository extends JpaRepository<NlpData, String> {
}
