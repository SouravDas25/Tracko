package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestSecurityConfig;
import com.trako.entities.JsonStore;
import com.trako.repositories.JsonStoreRepository;
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
public class JsonStoreIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JsonStoreRepository jsonStoreRepository;

    @BeforeEach
    public void setup() {
        jsonStoreRepository.deleteAll();
    }

    @Test
    public void testCreateSetting() throws Exception {
        JsonStore setting = new JsonStore();
        setting.setName("theme");
        setting.setValue("{\"mode\":\"dark\"}");

        mockMvc.perform(post("/api/json-store")
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

        mockMvc.perform(get("/api/json-store"))
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

        mockMvc.perform(get("/api/json-store/currency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("currency"))
                .andExpect(jsonPath("$.result.value").value("USD"));
    }

    @Test
    public void testGetSettingByNameNotFound() throws Exception {
        mockMvc.perform(get("/api/json-store/nonexistent"))
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

        mockMvc.perform(delete("/api/json-store/temp"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/json-store/temp"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateSettingWithComplexJson() throws Exception {
        JsonStore setting = new JsonStore();
        setting.setName("preferences");
        setting.setValue("{\"notifications\":{\"email\":true,\"push\":false},\"privacy\":{\"shareData\":false}}");

        mockMvc.perform(post("/api/json-store")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("newSetting"))
                .andExpect(jsonPath("$.result.value").value("newValue"));
    }
}
