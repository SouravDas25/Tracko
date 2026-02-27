package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.models.request.UserSaveRequest;
import com.trako.repositories.UsersRepository;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class UserCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private AuthenticationManager authenticationManager;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String regularToken;

    @BeforeEach
    public void setup() {
        usersRepository.deleteAll();

        // Create Admin User
        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setPhoneNo("9999999999");
        adminUser.setFireBaseId("admin_pass");
        adminUser.setIsAdmin(1);
        adminUser = usersRepository.save(adminUser);
        adminToken = "Bearer " + jwtTokenUtil.generateToken(new org.springframework.security.core.userdetails.User(adminUser.getPhoneNo(), adminUser.getFireBaseId(), Collections.emptyList()));

        // Create Regular User
        regularUser = new User();
        regularUser.setName("Regular User");
        regularUser.setPhoneNo("8888888888");
        regularUser.setFireBaseId("user_pass");
        regularUser.setIsAdmin(0);
        regularUser = usersRepository.save(regularUser);
        regularToken = "Bearer " + jwtTokenUtil.generateToken(new org.springframework.security.core.userdetails.User(regularUser.getPhoneNo(), regularUser.getFireBaseId(), Collections.emptyList()));
    }

    @Test
    public void testAdminCanCreateNewUser() throws Exception {
        UserSaveRequest request = new UserSaveRequest();
        request.setName("New User");
        request.setPhoneNo("7777777777");
        request.setFireBaseId("new_pass");

        mockMvc.perform(post("/api/user/save")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User newUser = usersRepository.findByPhoneNo("7777777777");
        assertNotNull(newUser);
        assertEquals("New User", newUser.getName());
    }

    @Test
    public void testRegularUserCannotCreateNewUser_UpdatesSelfInstead() throws Exception {
        UserSaveRequest request = new UserSaveRequest();
        request.setName("Updated Regular User");
        request.setPhoneNo("7777777777"); // Trying to create/hijack this number
        request.setFireBaseId("new_pass");

        mockMvc.perform(post("/api/user/save")
                .header("Authorization", regularToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify "7777777777" was NOT created
        User newUser = usersRepository.findByPhoneNo("7777777777");
        assertNull(newUser);

        // Verify regularUser was updated
        User updatedUser = usersRepository.findById(regularUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals("Updated Regular User", updatedUser.getName());
        assertEquals("8888888888", updatedUser.getPhoneNo()); // Phone should NOT change
    }
}
