package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Category;
import com.trako.entities.User;
import com.trako.models.request.CategorySaveRequest;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.UsersRepository;
import com.trako.util.JwtTokenUtil;
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

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
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

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = usersRepository.save(testUser);

        var principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getPassword(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);
    }

    @Test
    public void testCreateCategoryWithoutNameReturnsBadRequest() throws Exception {
        CategorySaveRequest req = new CategorySaveRequest();
        // name is null -> @NotNull should trigger 400

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateCategoryWithoutNameReturnsBadRequest() throws Exception {
        Category saved = new Category();
        saved.setName("HasName");
        saved.setUserId(testUser.getId());
        saved = categoryRepository.save(saved);

        CategorySaveRequest req = new CategorySaveRequest();
        // name not set -> should 400

        mockMvc.perform(put("/api/categories/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateCategoryWithoutAuthReturnsUnauthorized() throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "NoAuthCat");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAllCategories_doesNotReturnOtherUsersCategories() throws Exception {
        User other = new User();
        other.setName("OtherU");
        other.setPhoneNo("7777777777");
        other.setEmail("otheru@example.com");
        other.setPassword("other_pass");
        other = usersRepository.save(other);

        Category mine = new Category();
        mine.setName("MineCat");
        mine.setUserId(testUser.getId());
        categoryRepository.save(mine);

        Category foreign = new Category();
        foreign.setName("ForeignCat");
        foreign.setUserId(other.getId());
        categoryRepository.save(foreign);

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].name").value("MineCat"));
    }

    @Test
    public void testGetCategoryByIdUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("OtherCat");
        other.setPhoneNo("8888888888");
        other.setEmail("othercat@example.com");
        other.setPassword("othercat_pass");
        other = usersRepository.save(other);

        Category foreign = new Category();
        foreign.setName("ForeignCat");
        foreign.setUserId(other.getId());
        foreign = categoryRepository.save(foreign);

        mockMvc.perform(get("/api/categories/" + foreign.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateCategoryUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("UpdOtherCat");
        other.setPhoneNo("9999999999");
        other.setEmail("updothercat@example.com");
        other.setPassword("updothercat_pass");
        other = usersRepository.save(other);

        Category foreign = new Category();
        foreign.setName("ForeignUpdCat");
        foreign.setUserId(other.getId());
        foreign = categoryRepository.save(foreign);

        var body = new java.util.HashMap<String, Object>();
        body.put("name", "TryUpdate");

        mockMvc.perform(put("/api/categories/" + foreign.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteCategoryUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("DelOtherCat");
        other.setPhoneNo("6666666666");
        other.setEmail("delothercat@example.com");
        other.setPassword("delothercat_pass");
        other = usersRepository.save(other);

        Category foreign = new Category();
        foreign.setName("ForeignDelCat");
        foreign.setUserId(other.getId());
        foreign = categoryRepository.save(foreign);

        mockMvc.perform(delete("/api/categories/" + foreign.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetCategoryByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/categories/999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateCategory() throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Food");

        mockMvc.perform(post("/api/categories")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Food"))
                .andExpect(jsonPath("$.result.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.result.categoryType").value("EXPENSE"));
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

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearerToken))
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

        mockMvc.perform(get("/api/categories/" + saved.getId())
                        .header("Authorization", bearerToken))
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

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearerToken))
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

        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Updated Category");
        body.put("categoryType", "INCOME");

        mockMvc.perform(put("/api/categories/" + saved.getId())
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Updated Category"))
                .andExpect(jsonPath("$.result.categoryType").value("INCOME"));
    }

    @Test
    public void testDeleteCategory() throws Exception {
        Category category = new Category();
        category.setName("To Delete");
        category.setUserId(testUser.getId());
        Category saved = categoryRepository.save(category);

        mockMvc.perform(delete("/api/categories/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categories/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
