package com.trako.repositories;

import com.trako.entities.JsonStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JsonStoreRepository extends JpaRepository<JsonStore, Long> {

    List<JsonStore> findByUserId(String userId);

    Optional<JsonStore> findByUserIdAndName(String userId, String name);

    void deleteByUserIdAndName(String userId, String name);
}
