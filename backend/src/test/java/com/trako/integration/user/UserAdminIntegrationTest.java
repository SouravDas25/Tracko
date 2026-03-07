package com.trako.integration.user;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class UserAdminIntegrationTest extends BaseIntegrationTest {

    private String adminBearer;
    private String userBearer;
    private User adminUser;
    private User normalUser;

    @BeforeEach
    public void setup() {
        adminUser = new User();
        adminUser.setName("Admin");
        adminUser.setPhoneNo(generateUniquePhone());
        adminUser.setEmail("admin_" + adminUser.getPhoneNo() + "@mail.com");
        adminUser.setPassword("password");
        adminUser.setIsShadow(0);
        adminUser.setIsAdmin(1);
        adminUser = usersRepository.save(adminUser);

        normalUser = new User();
        normalUser.setName("Normal");
        normalUser.setPhoneNo(generateUniquePhone());
        normalUser.setEmail("normal_" + normalUser.getPhoneNo() + "@mail.com");
        normalUser.setPassword("password");
        normalUser.setIsShadow(0);
        normalUser.setIsAdmin(0);
        normalUser = usersRepository.save(normalUser);

        var adminPrincipal = new org.springframework.security.core.userdetails.User(
                adminUser.getPhoneNo(),
                adminUser.getPassword(),
                Collections.emptyList()
        );
        adminBearer = "Bearer " + jwtTokenUtil.generateToken(adminPrincipal);

        var userPrincipal = new org.springframework.security.core.userdetails.User(
                normalUser.getPhoneNo(),
                normalUser.getPassword(),
                Collections.emptyList()
        );
        userBearer = "Bearer " + jwtTokenUtil.generateToken(userPrincipal);
    }

    @Test
    public void adminCanCreateUserViaCreate() throws Exception {
        String phone = generateUniquePhone();
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Created User");
        body.put("phoneNo", phone);
        body.put("email", "created_" + phone + "@mail.com");
        body.put("password", "password");
        body.put("isShadow", 0);

        mockMvc.perform(post("/api/user/create")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", notNullValue()));

        User created = usersRepository.findByPhoneNo(phone);
        assertNotNull(created);
        assertEquals("Created User", created.getName());
    }

    @Test
    public void nonAdminCannotCreateOtherUserViaCreate() throws Exception {
        String phone = generateUniquePhone();
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Hacker");
        body.put("phoneNo", phone);
        body.put("email", "hacker_" + phone + "@mail.com");
        body.put("password", "password");
        body.put("isShadow", 0);

        mockMvc.perform(post("/api/user/create")
                        .header("Authorization", userBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());

        User notCreated = usersRepository.findByPhoneNo(phone);
        assertNull(notCreated);
    }

    @Test
    public void adminCanFetchOtherUserById() throws Exception {
        User target = new User();
        target.setName("Target");
        target.setPhoneNo("4444444444");
        target.setEmail("target@mail.com");
        target.setPassword("password");
        target.setIsShadow(0);
        target.setIsAdmin(0);
        target = usersRepository.save(target);

        mockMvc.perform(get("/api/user/" + target.getId())
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].id").value(target.getId()))
                .andExpect(jsonPath("$.result[0].name").value("Target"));
    }

    @Test
    public void nonAdminCannotFetchOtherUserById() throws Exception {
        User target = new User();
        target.setName("Target");
        target.setPhoneNo("4444444444");
        target.setEmail("target@mail.com");
        target.setPassword("password");
        target.setIsShadow(0);
        target.setIsAdmin(0);
        target = usersRepository.save(target);

        mockMvc.perform(get("/api/user/" + target.getId())
                        .header("Authorization", userBearer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void nonAdminListReturnsOnlySelf() throws Exception {
        mockMvc.perform(get("/api/user")
                        .header("Authorization", userBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    public void adminListReturnsAllUsers() throws Exception {
        mockMvc.perform(get("/api/user")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.result[?(@.id == '" + adminUser.getId() + "')]").exists())
                .andExpect(jsonPath("$.result[?(@.id == '" + normalUser.getId() + "')]").exists());
    }

    @Test
    public void byPhoneNoRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .param("phone_no", generateUniquePhone()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void nonAdminCanUpdateOwnProfileViaMeEndpoint() throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Normal Updated");
        body.put("email", "normal.updated@mail.com");
        body.put("profilePic", "https://example.com/pic.jpg");

        mockMvc.perform(post("/api/user/me")
                        .header("Authorization", userBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        User normal = usersRepository.findById(normalUser.getId()).orElse(null);
        assertNotNull(normal);
        assertEquals("Normal Updated", normal.getName());
        assertEquals("normal.updated@mail.com", normal.getEmail());
        assertEquals(normalUser.getPhoneNo(), normal.getPhoneNo());
    }
}
