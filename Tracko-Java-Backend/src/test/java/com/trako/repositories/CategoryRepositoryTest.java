package com.trako.repositories;

import com.trako.entities.Category;
import com.trako.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    public void testSaveCategory() {
        Category category = new Category();
        category.setName("Food");
        category.setUserId(testUser.getId());

        Category saved = categoryRepository.save(category);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Food");
        assertThat(saved.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    public void testFindByUserId() {
        Category category1 = new Category();
        category1.setName("Food");
        category1.setUserId(testUser.getId());
        entityManager.persist(category1);

        Category category2 = new Category();
        category2.setName("Travel");
        category2.setUserId(testUser.getId());
        entityManager.persist(category2);

        entityManager.flush();

        List<Category> categories = categoryRepository.findByUserId(testUser.getId());

        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(Category::getName).containsExactlyInAnyOrder("Food", "Travel");
    }

    @Test
    public void testFindById() {
        Category category = new Category();
        category.setName("Grocery");
        category.setUserId(testUser.getId());
        Category saved = entityManager.persist(category);
        entityManager.flush();

        Category found = categoryRepository.findById(saved.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Grocery");
    }

    @Test
    public void testDeleteCategory() {
        Category category = new Category();
        category.setName("Temporary");
        category.setUserId(testUser.getId());
        Category saved = entityManager.persist(category);
        entityManager.flush();

        categoryRepository.deleteById(saved.getId());

        Category found = categoryRepository.findById(saved.getId()).orElse(null);
        assertThat(found).isNull();
    }
}
