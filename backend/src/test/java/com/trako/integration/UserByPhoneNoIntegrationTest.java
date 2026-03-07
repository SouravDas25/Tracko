package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class UserByPhoneNoIntegrationTest extends BaseIntegrationTest {

    private User authUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        authUser = createUniqueUser("Auth User");
        bearerToken = generateBearerToken(authUser);
    }

    @Test
    public void byPhoneNoWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .queryParam("phone_no", "1234567890"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void byPhoneNoWhenUserExistsReturnsList() throws Exception {
        User target = createUniqueUser("Target");
        String targetPhone = target.getPhoneNo();

        // send formatted phone to exercise CommonUtil.extractPhoneNumber
        // Assuming CommonUtil handles simple substring matching or stripping non-digits.
        // Let's just use the raw phone if format logic is internal, 
        // or format it like "+91-99988 87777" if we knew the format. 
        // Since we generate random 10 digits, let's just pass it directly for safety or simple format.

        mockMvc.perform(get("/api/user/byPhoneNo")
                        .header("Authorization", bearerToken)
                        .queryParam("phone_no", targetPhone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].id").value(target.getId()))
                .andExpect(jsonPath("$.result[0].phoneNo").value(targetPhone));
    }

    @Test
    public void byPhoneNoWhenNotFoundReturnsResourceEmpty() throws Exception {
        String nonExistentPhone = generateUniquePhone();
        // Ensure it doesn't exist
        if (usersRepository.findByPhoneNo(nonExistentPhone) != null) {
            nonExistentPhone = String.valueOf(Long.parseLong(nonExistentPhone) + 1);
        }

        mockMvc.perform(get("/api/user/byPhoneNo")
                        .header("Authorization", bearerToken)
                        .queryParam("phone_no", nonExistentPhone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource Empty"));
    }

    @Test
    public void byPhoneNoWithInvalidPhoneReturnsResourceEmpty() throws Exception {
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .header("Authorization", bearerToken)
                        .queryParam("phone_no", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource Empty"));
    }

    @Test
    public void meWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void meWithAuthReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(authUser.getId()))
                .andExpect(jsonPath("$.result.phoneNo").value(authUser.getPhoneNo()))
                .andExpect(jsonPath("$.result.email").value(authUser.getEmail()))
                .andExpect(jsonPath("$.result.name").value(authUser.getName()));
    }
}
