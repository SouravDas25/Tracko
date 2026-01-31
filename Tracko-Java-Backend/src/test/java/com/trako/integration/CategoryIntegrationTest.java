package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestSecurityConfig;
import com.trako.entities.Category;
import com.trako.entities.User;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
public class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UsersRepository usersRepository;

    private User testUser;

    @BeforeEach
    public void setup() {
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser = usersRepository.save(testUser);
    }

    @Test
    public void testCreateCategory() throws Exception {
        Category category = new Category();
        category.setName("Food");
        category.setUserId(testUser.getId());

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Food"))
                .andExpect(jsonPath("$.result.userId").value(testUser.getId()));
    }

    @Test
    public void testGetAllCategories() throws Exception {
        Category category1 = new Category();
        category1.setName("Food");
        category1.setUserId(testUser.getId());
        categoryRepository.save(category1);

        Category category2 = new Category();
        category2.setName("Travel");
        category2.setUserId(testUser.getId());
        categoryRepository.save(category2);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].name", containsInAnyOrder("Food", "Travel")));
    }

    @Test
    public void testGetCategoryById() throws Exception {
        Category category = new Category();
        category.setName("Grocery");
        category.setUserId(testUser.getId());
        Category saved = categoryRepository.save(category);

        mockMvc.perform(get("/api/categories/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Grocery"));
    }

    @Test
    public void testGetCategoriesByUserId() throws Exception {
        Category category1 = new Category();
        category1.setName("Food");
        category1.setUserId(testUser.getId());
        categoryRepository.save(category1);

        Category category2 = new Category();
        category2.setName("Entertainment");
        category2.setUserId(testUser.getId());
        categoryRepository.save(category2);

        mockMvc.perform(get("/api/categories/user/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].userId", everyItem(is(testUser.getId()))));
    }

    @Test
    public void testUpdateCategory() throws Exception {
        Category category = new Category();
        category.setName("Old Category");
        category.setUserId(testUser.getId());
        Category saved = categoryRepository.save(category);

        saved.setName("Updated Category");

        mockMvc.perform(put("/api/categories/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Updated Category"));
    }

    @Test
    public void testDeleteCategory() throws Exception {
        Category category = new Category();
        category.setName("To Delete");
        category.setUserId(testUser.getId());
        Category saved = categoryRepository.save(category);

        mockMvc.perform(delete("/api/categories/" + saved.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categories/" + saved.getId()))
                .andExpect(status().isNotFound());
    }
}
