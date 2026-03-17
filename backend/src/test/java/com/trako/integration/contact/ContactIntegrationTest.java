package com.trako.integration.contact;

import com.trako.entities.Contact;
import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import com.trako.models.request.ContactSaveRequest;
import com.trako.repositories.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ContactIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ContactRepository contactRepository;

    private User testUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");
        bearerToken = generateBearerToken(testUser);
    }

    @Test
    public void listMineWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getOneWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/contacts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createWithoutAuthReturnsUnauthorized() throws Exception {
        ContactSaveRequest req = new ContactSaveRequest();
        req.setName("NoAuth");

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateWithoutAuthReturnsUnauthorized() throws Exception {
        ContactSaveRequest req = new ContactSaveRequest();
        req.setName("NoAuth");

        mockMvc.perform(put("/api/contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/contacts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createListGetUpdateDeleteHappyPath() throws Exception {
        ContactSaveRequest create = new ContactSaveRequest();
        create.setName("Alice");
        create.setPhoneNo("1112223333");
        create.setEmail("alice@example.com");

        String createResponse = mockMvc.perform(post("/api/contacts")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contact created successfully"))
                .andExpect(jsonPath("$.result.id").isNotEmpty())
                .andExpect(jsonPath("$.result.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.result.name").value("Alice"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long createdId = objectMapper.readTree(createResponse).path("result").path("id").asLong();

        mockMvc.perform(get("/api/contacts")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].id").value(createdId))
                .andExpect(jsonPath("$.result[0].name").value("Alice"));

        mockMvc.perform(get("/api/contacts/" + createdId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(createdId))
                .andExpect(jsonPath("$.result.name").value("Alice"));

        ContactSaveRequest update = new ContactSaveRequest();
        update.setName("Alice Updated");
        update.setPhoneNo("9998887777");
        update.setEmail("alice2@example.com");

        mockMvc.perform(put("/api/contacts/" + createdId)
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contact updated successfully"))
                .andExpect(jsonPath("$.result.id").value(createdId))
                .andExpect(jsonPath("$.result.name").value("Alice Updated"))
                .andExpect(jsonPath("$.result.phoneNo").value("9998887777"))
                .andExpect(jsonPath("$.result.email").value("alice2@example.com"));

        mockMvc.perform(delete("/api/contacts/" + createdId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contact deleted successfully"));

        mockMvc.perform(get("/api/contacts/" + createdId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Contact not found"));
    }

    @Test
    public void createWithMissingNameReturnsBadRequest() throws Exception {
        ContactSaveRequest req = new ContactSaveRequest();
        req.setPhoneNo("1112223333");

        mockMvc.perform(post("/api/contacts")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateWithMissingNameReturnsBadRequest() throws Exception {
        Contact contact = new Contact();
        contact.setUserId(testUser.getId());
        contact.setName("Bob");
        contact.setPhoneNo("2223334444");
        contact = contactRepository.save(contact);

        ContactSaveRequest req = new ContactSaveRequest();
        req.setEmail("bob@example.com");

        mockMvc.perform(put("/api/contacts/" + contact.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getUpdateDeleteForeignContactReturnsNotFound() throws Exception {
        User other = createUniqueUser("Other");

        Contact foreign = new Contact();
        foreign.setUserId(other.getId());
        foreign.setName("Foreign");
        foreign.setPhoneNo("7776665555");
        foreign = contactRepository.save(foreign);

        mockMvc.perform(get("/api/contacts/" + foreign.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Contact not found"));

        ContactSaveRequest update = new ContactSaveRequest();
        update.setName("Try Update");

        mockMvc.perform(put("/api/contacts/" + foreign.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Contact not found"));

        mockMvc.perform(delete("/api/contacts/" + foreign.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Contact not found"));
    }

    @Test
    public void getUpdateDeleteNotFoundReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/contacts/999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());

        ContactSaveRequest update = new ContactSaveRequest();
        update.setName("Missing");

        mockMvc.perform(put("/api/contacts/999999")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/contacts/999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
