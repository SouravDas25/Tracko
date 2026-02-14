package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.JsonStore;
import com.trako.entities.User;
import com.trako.repositories.JsonStoreRepository;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class JsonStoreIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JsonStoreRepository jsonStoreRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        jsonStoreRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser.setFireBaseId("password");
        testUser = usersRepository.save(testUser);

        var principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getFireBaseId(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);
    }

    @Test
    public void testCreateSetting() throws Exception {
        JsonStore setting = new JsonStore();
        setting.setName("theme");
        setting.setValue("{\"mode\":\"dark\"}");

        mockMvc.perform(post("/api/json-store")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("theme"))
                .andExpect(jsonPath("$.result.value").value("{\"mode\":\"dark\"}"));
    }

    @Test
    public void testGetAllSettings() throws Exception {
        JsonStore setting1 = new JsonStore();
        setting1.setName("theme");
        setting1.setValue("{\"mode\":\"dark\"}");
        jsonStoreRepository.save(setting1);

        JsonStore setting2 = new JsonStore();
        setting2.setName("language");
        setting2.setValue("en");
        jsonStoreRepository.save(setting2);

        mockMvc.perform(get("/api/json-store")
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].name", containsInAnyOrder("theme", "language")));
    }

    @Test
    public void testGetSettingByName() throws Exception {
        JsonStore setting = new JsonStore();
        setting.setName("currency");
        setting.setValue("USD");
        jsonStoreRepository.save(setting);

        mockMvc.perform(get("/api/json-store/currency")
                .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("currency"))
                .andExpect(jsonPath("$.result.value").value("USD"));
    }

    @Test
    public void testGetSettingByNameNotFound() throws Exception {
        mockMvc.perform(get("/api/json-store/nonexistent")
                .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSetting() throws Exception {
        JsonStore setting = new JsonStore();
        setting.setName("theme");
        setting.setValue("{\"mode\":\"light\"}");
        jsonStoreRepository.save(setting);

        JsonStore updated = new JsonStore();
        updated.setName("theme");
        updated.setValue("{\"mode\":\"dark\"}");

        mockMvc.perform(put("/api/json-store/theme")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("{\"mode\":\"dark\"}"));
    }

    @Test
    public void testDeleteSetting() throws Exception {
        JsonStore setting = new JsonStore();
        setting.setName("temp");
        setting.setValue("temporary");
        jsonStoreRepository.save(setting);

        mockMvc.perform(delete("/api/json-store/temp")
                .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/json-store/temp")
                .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateSettingWithComplexJson() throws Exception {
        JsonStore setting = new JsonStore();
        setting.setName("preferences");
        setting.setValue("{\"notifications\":{\"email\":true,\"push\":false},\"privacy\":{\"shareData\":false}}");

        mockMvc.perform(post("/api/json-store")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("preferences"))
                .andExpect(jsonPath("$.result.value").isNotEmpty());
    }

    @Test
    public void testUpdateNonExistentSettingCreatesNew() throws Exception {
        JsonStore setting = new JsonStore();
        setting.setName("newSetting");
        setting.setValue("newValue");

        mockMvc.perform(put("/api/json-store/newSetting")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("newSetting"))
                .andExpect(jsonPath("$.result.value").value("newValue"));
    }
}
