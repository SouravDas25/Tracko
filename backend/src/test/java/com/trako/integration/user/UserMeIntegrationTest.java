package com.trako.integration.user;

import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserMeIntegrationTest extends BaseIntegrationTest {

    private User authUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        authUser = createUniqueUser("Auth User");
        bearerToken = generateBearerToken(authUser);
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

    @Test
    public void byPhoneNoEndpointRemovedReturnsUnauthorized() throws Exception {
        // The /byPhoneNo lookup was removed (user-enumeration fix). With the endpoint gone, an
        // authenticated request hits no handler and is forwarded to /error; JwtRequestFilter
        // (OncePerRequestFilter) is skipped on the error dispatch, so the security chain's entry
        // point returns 401. Before removal this same authenticated call returned 200 with the
        // target user's profile, so a 401 here proves the route is gone.
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .header("Authorization", bearerToken)
                        .queryParam("phone_no", "1234567890"))
                .andExpect(status().isUnauthorized());
    }
}
