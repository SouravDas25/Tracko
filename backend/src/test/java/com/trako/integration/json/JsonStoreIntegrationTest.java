package com.trako.integration.json;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.JsonStore;
import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import com.trako.repositories.JsonStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class JsonStoreIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JsonStoreRepository jsonStoreRepository;

    private User testUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");
        bearerToken = generateBearerToken(testUser);
    }

    @Test
    public void getAllWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/json-store"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createAndRetrieveJsonStore() throws Exception {
        JsonStore store = new JsonStore();
        store.setName("test-config");
        store.setValue("{\"key\": \"value\"}");

        mockMvc.perform(post("/api/json-store")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("test-config"))
                .andExpect(jsonPath("$.result.value").value("{\"key\": \"value\"}"));

        mockMvc.perform(get("/api/json-store/test-config")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("test-config"))
                .andExpect(jsonPath("$.result.value").value("{\"key\": \"value\"}"));
    }

    @Test
    public void updateJsonStore() throws Exception {
        JsonStore store = new JsonStore();
        store.setName("update-config");
        store.setValue("initial");
        jsonStoreRepository.save(store);

        store.setValue("updated");

        mockMvc.perform(put("/api/json-store/update-config")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(store)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("updated"));
    }

    @Test
    public void deleteJsonStore() throws Exception {
        JsonStore store = new JsonStore();
        store.setName("delete-config");
        store.setValue("value");
        jsonStoreRepository.save(store);

        mockMvc.perform(delete("/api/json-store/delete-config")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/json-store/delete-config")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
