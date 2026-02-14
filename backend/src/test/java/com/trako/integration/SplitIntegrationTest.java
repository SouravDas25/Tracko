package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.repositories.*;
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
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class SplitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SplitRepository splitRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User testUser;
    private User otherUser;
    private Transaction testTransaction;
    private String bearerToken;
    private String otherBearerToken;
    private Contact testContact;

    @BeforeEach
    public void setup() {
        splitRepository.deleteAll();
        transactionRepository.deleteAll();
        contactRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();
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

        otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setPhoneNo("5555555555");
        otherUser.setEmail("other@example.com");
        otherUser.setFireBaseId("password");
        otherUser = usersRepository.save(otherUser);

        var otherPrincipal = new org.springframework.security.core.userdetails.User(
                otherUser.getPhoneNo(),
                otherUser.getFireBaseId(),
                Collections.emptyList()
        );
        otherBearerToken = "Bearer " + jwtTokenUtil.generateToken(otherPrincipal);

        Account testAccount = new Account();
        testAccount.setName("Savings");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        Category testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);

        testTransaction = new Transaction();
        testTransaction.setTransactionType(1);
        testTransaction.setName("Dinner");
        testTransaction.setAmount(100.00);
        testTransaction.setDate(new Date());
        testTransaction.setAccountId(testAccount.getId());
        testTransaction.setCategoryId(testCategory.getId());
        testTransaction = transactionRepository.save(testTransaction);

        testContact = new Contact();
        testContact.setUserId(testUser.getId());
        testContact.setName("C1");
        testContact.setPhoneNo("9990001111");
        testContact = contactRepository.save(testContact);
    }

    @Test
    public void testCreateSplit() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(0);

        mockMvc.perform(post("/api/splits")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(split)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(50.00))
                .andExpect(jsonPath("$.result.isSettled").value(0));
    }

    @Test
    public void testCreateSplit_missingTransactionId_returnsBadRequest() throws Exception {
        Split split = new Split();
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(0);

        mockMvc.perform(post("/api/splits")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(split)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetAllSplits() throws Exception {
        Split split1 = new Split();
        split1.setTransactionId(testTransaction.getId());
        split1.setUserId(testUser.getId());
        split1.setAmount(50.00);
        splitRepository.save(split1);

        Split split2 = new Split();
        split2.setTransactionId(testTransaction.getId());
        split2.setUserId(testUser.getId());
        split2.setAmount(50.00);
        splitRepository.save(split2);

        mockMvc.perform(get("/api/splits")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)));
    }

    @Test
    public void testGetSplitById() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(75.00);
        Split saved = splitRepository.save(split);

        mockMvc.perform(get("/api/splits/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(75.00));
    }

    @Test
    public void testGetSplitsByTransactionId() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(100.00);
        splitRepository.save(split);

        mockMvc.perform(get("/api/splits/transaction/" + testTransaction.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].transactionId").value(testTransaction.getId().intValue()));
    }

    @Test
    public void testGetSplitsByUserId() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        splitRepository.save(split);

        mockMvc.perform(get("/api/splits/user/" + testUser.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].userId").value(testUser.getId()));
    }

    @Test
    public void testGetUnsettledSplitsByUserId() throws Exception {
        Split split1 = new Split();
        split1.setTransactionId(testTransaction.getId());
        split1.setUserId(testUser.getId());
        split1.setAmount(50.00);
        split1.setIsSettled(0);
        splitRepository.save(split1);

        Split split2 = new Split();
        split2.setTransactionId(testTransaction.getId());
        split2.setUserId(testUser.getId());
        split2.setAmount(50.00);
        split2.setIsSettled(1);
        splitRepository.save(split2);

        mockMvc.perform(get("/api/splits/user/" + testUser.getId() + "/unsettled")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].isSettled").value(0));
    }

    @Test
    public void testGetSplitsByUserId_whenDifferentUserId_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/splits/user/" + otherUser.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetSplitsByContactId() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setContactId(testContact.getId());
        split.setAmount(25.00);
        split.setIsSettled(0);
        splitRepository.save(split);

        mockMvc.perform(get("/api/splits/contact/" + testContact.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].contactId").value(testContact.getId().intValue()));
    }

    @Test
    public void testGetSplitsByContactId_whenContactOwnedByAnotherUser_returnsUnauthorized() throws Exception {
        Contact otherContact = new Contact();
        otherContact.setUserId(otherUser.getId());
        otherContact.setName("OtherContact");
        otherContact = contactRepository.save(otherContact);

        mockMvc.perform(get("/api/splits/contact/" + otherContact.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetUnsettledSplitsByContactId_filtersSettled() throws Exception {
        Split unsettled = new Split();
        unsettled.setTransactionId(testTransaction.getId());
        unsettled.setUserId(testUser.getId());
        unsettled.setContactId(testContact.getId());
        unsettled.setAmount(10.00);
        unsettled.setIsSettled(0);
        splitRepository.save(unsettled);

        Split settled = new Split();
        settled.setTransactionId(testTransaction.getId());
        settled.setUserId(testUser.getId());
        settled.setContactId(testContact.getId());
        settled.setAmount(10.00);
        settled.setIsSettled(1);
        splitRepository.save(settled);

        mockMvc.perform(get("/api/splits/contact/" + testContact.getId() + "/unsettled")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].isSettled").value(0));
    }

    @Test
    public void testSettleSplit() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(0);
        Split saved = splitRepository.save(split);

        mockMvc.perform(patch("/api/splits/settle/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Split settled = splitRepository.findById(saved.getId()).orElse(null);
        assertThat(settled).isNotNull();
        assertThat(settled.getIsSettled()).isEqualTo(1);
        assertThat(settled.getSettledAt()).isNotNull();
    }

    @Test
    public void testUnsettleSplit() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(1);
        split.setSettledAt(new Date());
        Split saved = splitRepository.save(split);

        mockMvc.perform(patch("/api/splits/unsettle/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Split unsettled = splitRepository.findById(saved.getId()).orElse(null);
        assertThat(unsettled).isNotNull();
        assertThat(unsettled.getIsSettled()).isEqualTo(0);
        assertThat(unsettled.getSettledAt()).isNull();
    }

    @Test
    public void testDeleteSplit() throws Exception {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        Split saved = splitRepository.save(split);

        mockMvc.perform(delete("/api/splits/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/splits/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
