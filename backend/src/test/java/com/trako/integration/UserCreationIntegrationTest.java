package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.models.request.UserSaveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class UserCreationIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private AuthenticationManager authenticationManager;

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String regularToken;
    private String adminPhone;
    private String regularPhone;

    @BeforeEach
    public void setup() {
        // Create Admin User
        adminUser = new User();
        adminUser.setName("Admin User");
        adminPhone = generateUniquePhone();
        adminUser.setPhoneNo(adminPhone);
        adminUser.setPassword("admin_pass");
        adminUser.setIsAdmin(1);
        adminUser = usersRepository.save(adminUser);
        adminToken = "Bearer " + jwtTokenUtil.generateToken(new org.springframework.security.core.userdetails.User(adminUser.getPhoneNo(), adminUser.getPassword(), Collections.emptyList()));

        // Create Regular User
        regularUser = new User();
        regularUser.setName("Regular User");
        regularPhone = generateUniquePhone();
        regularUser.setPhoneNo(regularPhone);
        regularUser.setPassword("user_pass");
        regularUser.setIsAdmin(0);
        regularUser = usersRepository.save(regularUser);
        regularToken = "Bearer " + jwtTokenUtil.generateToken(new org.springframework.security.core.userdetails.User(regularUser.getPhoneNo(), regularUser.getPassword(), Collections.emptyList()));
    }

    @Test
    public void testAdminCanCreateNewUser() throws Exception {
        UserSaveRequest request = new UserSaveRequest();
        request.setName("New User");
        String newUserPhone = generateUniquePhone();
        request.setPhoneNo(newUserPhone);
        request.setPassword("new_pass");

        mockMvc.perform(post("/api/user/create")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User newUser = usersRepository.findByPhoneNo(newUserPhone);
        assertNotNull(newUser);
        assertEquals("New User", newUser.getName());
    }

    @Test
    public void testRegularUserCannotCreateNewUser() throws Exception {
        UserSaveRequest request = new UserSaveRequest();
        request.setName("Hacker User");
        String newUserPhone = generateUniquePhone();
        request.setPhoneNo(newUserPhone);
        request.setPassword("new_pass");

        mockMvc.perform(post("/api/user/create")
                        .header("Authorization", regularToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Verify "newUserPhone" was NOT created
        User newUser = usersRepository.findByPhoneNo(newUserPhone);
        assertNull(newUser);

        // Verify regularUser was NOT modified
        User updatedUser = usersRepository.findById(regularUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals("Regular User", updatedUser.getName());
        assertEquals(regularPhone, updatedUser.getPhoneNo());
    }
}
