package com.trako.repositories;

import com.trako.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserId(String userId);
    List<Category> findByUserIdAndName(String userId, String name);
}
