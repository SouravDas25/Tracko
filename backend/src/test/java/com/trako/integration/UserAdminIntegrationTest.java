package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class UserAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String adminBearer;
    private String userBearer;

    @BeforeEach
    public void setup() {
        usersRepository.deleteAll();

        User admin = new User();
        admin.setName("Admin");
        admin.setPhoneNo("0000000000");
        admin.setEmail("admin@mail.com");
        admin.setFireBaseId("password");
        admin.setIsShadow(0);
        admin.setIsAdmin(1);
        usersRepository.save(admin);

        User normal = new User();
        normal.setName("Normal");
        normal.setPhoneNo("1111111111");
        normal.setEmail("normal@mail.com");
        normal.setFireBaseId("password");
        normal.setIsShadow(0);
        normal.setIsAdmin(0);
        usersRepository.save(normal);

        var adminPrincipal = new org.springframework.security.core.userdetails.User(
                admin.getPhoneNo(),
                admin.getFireBaseId(),
                Collections.emptyList()
        );
        adminBearer = "Bearer " + jwtTokenUtil.generateToken(adminPrincipal);

        var userPrincipal = new org.springframework.security.core.userdetails.User(
                normal.getPhoneNo(),
                normal.getFireBaseId(),
                Collections.emptyList()
        );
        userBearer = "Bearer " + jwtTokenUtil.generateToken(userPrincipal);
    }

    @Test
    public void adminCanCreateUserViaSave() throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Created User");
        body.put("phoneNo", "2222222222");
        body.put("email", "created@mail.com");
        body.put("uuid", "password");
        body.put("isShadow", 0);

        mockMvc.perform(post("/api/user/save")
                .header("Authorization", adminBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", notNullValue()));

        User created = usersRepository.findByPhoneNo("2222222222");
        org.junit.jupiter.api.Assertions.assertNotNull(created);
        org.junit.jupiter.api.Assertions.assertEquals("Created User", created.getName());
    }

    @Test
    public void nonAdminCannotCreateOtherUserViaSave() throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Hacker");
        body.put("phoneNo", "3333333333");
        body.put("email", "hacker@mail.com");
        body.put("uuid", "password");
        body.put("isShadow", 0);

        mockMvc.perform(post("/api/user/save")
                .header("Authorization", userBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        User notCreated = usersRepository.findByPhoneNo("3333333333");
        org.junit.jupiter.api.Assertions.assertNull(notCreated);
    }

    @Test
    public void adminCanFetchOtherUserById() throws Exception {
        User target = new User();
        target.setName("Target");
        target.setPhoneNo("4444444444");
        target.setEmail("target@mail.com");
        target.setFireBaseId("password");
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
        target.setFireBaseId("password");
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].phoneNo").value("1111111111"));
    }

    @Test
    public void byPhoneNoRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .param("phone_no", "1111111111"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void nonAdminSaveUpdatesSelfOnly() throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Normal Updated");
        body.put("phoneNo", "3333333333");
        body.put("email", "normal.updated@mail.com");
        body.put("uuid", "password");
        body.put("isShadow", 0);

        mockMvc.perform(post("/api/user/save")
                        .header("Authorization", userBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        User shouldNotExist = usersRepository.findByPhoneNo("3333333333");
        org.junit.jupiter.api.Assertions.assertNull(shouldNotExist);

        User normal = usersRepository.findByPhoneNo("1111111111");
        org.junit.jupiter.api.Assertions.assertNotNull(normal);
        org.junit.jupiter.api.Assertions.assertEquals("Normal Updated", normal.getName());
        org.junit.jupiter.api.Assertions.assertEquals("normal.updated@mail.com", normal.getEmail());
    }
}
