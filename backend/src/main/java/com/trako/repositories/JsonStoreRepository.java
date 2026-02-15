package com.trako.repositories;

import com.trako.entities.JsonStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JsonStoreRepository extends JpaRepository<JsonStore, String> {
}
