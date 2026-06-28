package com.trako.integration.json;

import com.trako.entities.JsonStore;
import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import com.trako.repositories.JsonStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class JsonStoreIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JsonStoreRepository jsonStoreRepository;

    private User testUser;
    private String bearerToken;

    private User otherUser;
    private String otherToken;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");
        bearerToken = generateBearerToken(testUser);

        otherUser = createUniqueUser("Other User");
        otherToken = generateBearerToken(otherUser);
    }

    private JsonStore saveStoreFor(User user, String name, String value) {
        JsonStore store = new JsonStore();
        store.setUserId(user.getId());
        store.setName(name);
        store.setValue(value);
        return jsonStoreRepository.save(store);
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
        JsonStore store = saveStoreFor(testUser, "update-config", "initial");

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
        saveStoreFor(testUser, "delete-config", "value");

        mockMvc.perform(delete("/api/json-store/delete-config")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/json-store/delete-config")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getByNameForAnotherUsersKeyReturnsNotFound() throws Exception {
        saveStoreFor(testUser, "secret-config", "owner-only");

        mockMvc.perform(get("/api/json-store/secret-config")
                        .header("Authorization", otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void sameNameHoldsIndependentValuesPerUser() throws Exception {
        saveStoreFor(testUser, "shared", "value-A");
        saveStoreFor(otherUser, "shared", "value-B");

        mockMvc.perform(get("/api/json-store/shared")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("value-A"));

        mockMvc.perform(get("/api/json-store/shared")
                        .header("Authorization", otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("value-B"));
    }

    @Test
    public void getAllReturnsOnlyCallersEntries() throws Exception {
        saveStoreFor(testUser, "a1", "1");
        saveStoreFor(testUser, "a2", "2");
        saveStoreFor(otherUser, "b1", "3");

        mockMvc.perform(get("/api/json-store")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].name", containsInAnyOrder("a1", "a2")));
    }

    @Test
    public void updatingSharedNameDoesNotAffectOtherUser() throws Exception {
        saveStoreFor(testUser, "shared", "value-A");
        saveStoreFor(otherUser, "shared", "value-B");

        JsonStore body = new JsonStore();
        body.setName("shared");
        body.setValue("value-B-updated");

        mockMvc.perform(put("/api/json-store/shared")
                        .header("Authorization", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("value-B-updated"));

        // The owning user's value is untouched.
        mockMvc.perform(get("/api/json-store/shared")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("value-A"));
    }

    @Test
    public void deletingSharedNameLeavesOtherUsersRowIntact() throws Exception {
        saveStoreFor(testUser, "shared", "value-A");
        saveStoreFor(otherUser, "shared", "value-B");

        mockMvc.perform(delete("/api/json-store/shared")
                        .header("Authorization", otherToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/json-store/shared")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("value-A"));
    }

    @Test
    public void clientSuppliedIdAndUserIdInBodyAreIgnored() throws Exception {
        JsonStore victim = saveStoreFor(testUser, "victim", "secret");

        // Another user posts a body carrying the victim's row id and userId,
        // attempting to hijack/overwrite it. Both fields must be ignored.
        String maliciousBody = "{\"name\":\"evil\",\"value\":\"x\",\"id\":" + victim.getId()
                + ",\"userId\":\"" + testUser.getId() + "\"}";

        mockMvc.perform(post("/api/json-store")
                        .header("Authorization", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousBody))
                .andExpect(status().isOk());

        // Victim's row is unchanged and still owned by the original user.
        mockMvc.perform(get("/api/json-store/victim")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("secret"));

        // The attacker only created a row in their own namespace.
        mockMvc.perform(get("/api/json-store/evil")
                        .header("Authorization", otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.value").value("x"));
        mockMvc.perform(get("/api/json-store/evil")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
