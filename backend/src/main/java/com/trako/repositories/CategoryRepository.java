package com.trako.repositories;

import com.trako.entities.Category;
import com.trako.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserId(String userId);

    List<Category> findByUserIdAndCategoryType(String userId, CategoryType categoryType);

    List<Category> findByUserIdOrderByNameAsc(String userId);

    List<Category> findByUserIdAndCategoryTypeOrderByNameAsc(String userId, CategoryType categoryType);

    List<Category> findByUserIdAndName(String userId, String name);

    boolean existsByUserIdAndNameIgnoreCase(String userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(String userId, String name, Long id);

    boolean existsByUserIdAndCategoryTypeAndNameIgnoreCase(String userId, CategoryType categoryType, String name);

    boolean existsByUserIdAndCategoryTypeAndNameIgnoreCaseAndIdNot(String userId, CategoryType categoryType, String name, Long id);

    @Modifying
    @Query("DELETE FROM Category c WHERE c.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
