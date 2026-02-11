package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.dtos.CategoryStatDTO;
import com.trako.dtos.CategoryStatsResponseDTO;
import com.trako.dtos.StatsPointDTO;
import com.trako.dtos.StatsResponseDTO;
import com.trako.repositories.UsersRepository;
import com.trako.services.StatsService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
public class StatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UsersRepository usersRepository;

    @MockBean
    private StatsService statsService;

    private String bearerToken;

    @BeforeEach
    public void setup() {
        usersRepository.deleteAll();
        var user = new com.trako.entities.User();
        user.setName("StatsUser");
        user.setPhoneNo("9090909090");
        user.setEmail("stats@example.com");
        user.setFireBaseId("pass");
        usersRepository.save(user);

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                user.getPhoneNo(), user.getFireBaseId(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        StatsResponseDTO dto = new StatsResponseDTO(
                "weekly",
                1,
                "2026-01-01",
                "2026-01-07",
                123.0,
                List.of(new StatsPointDTO("2026-01-01", 123.0)),
                List.of(new CategoryStatDTO(10L, "Food", 123.0))
        );
        when(statsService.getStats(anyString(), any(StatsService.Range.class), anyInt(), any())).thenReturn(dto);

        CategoryStatsResponseDTO catDto = new CategoryStatsResponseDTO(
                "weekly",
                1,
                10L,
                "2026-01-01",
                "2026-01-07",
                50.0,
                List.of(new StatsPointDTO("2026-01-01", 50.0))
        );
        when(statsService.getCategoryStats(anyString(), any(StatsService.Range.class), anyInt(), any(), anyLong()))
                .thenReturn(catDto);
    }

    @Test
    public void summaryWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/stats/summary")
                        .queryParam("range", "weekly")
                        .queryParam("transactionType", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void categorySummaryWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/stats/category-summary")
                        .queryParam("range", "weekly")
                        .queryParam("transactionType", "1")
                        .queryParam("categoryId", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void summaryInvalidRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/stats/summary")
                        .header("Authorization", bearerToken)
                        .queryParam("range", "bad")
                        .queryParam("transactionType", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid range. Use weekly|monthly|yearly"));
    }

    @Test
    public void summaryInvalidTransactionTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/stats/summary")
                        .header("Authorization", bearerToken)
                        .queryParam("range", "weekly")
                        .queryParam("transactionType", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid transactionType. Use 1 (DEBIT) or 2 (CREDIT)"));
    }

    @Test
    public void categorySummaryInvalidCategoryIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/stats/category-summary")
                        .header("Authorization", bearerToken)
                        .queryParam("range", "weekly")
                        .queryParam("transactionType", "1")
                        .queryParam("categoryId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid categoryId"));
    }

    @Test
    public void summaryBadDateFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/stats/summary")
                        .header("Authorization", bearerToken)
                        .queryParam("range", "weekly")
                        .queryParam("transactionType", "1")
                        .queryParam("date", "2026/01/01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void categorySummaryBadDateFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/stats/category-summary")
                        .header("Authorization", bearerToken)
                        .queryParam("range", "weekly")
                        .queryParam("transactionType", "1")
                        .queryParam("categoryId", "10")
                        .queryParam("date", "2026/01/01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void summaryHappyPathReturnsDto() throws Exception {
        mockMvc.perform(get("/api/stats/summary")
                        .header("Authorization", bearerToken)
                        .queryParam("range", "weekly")
                        .queryParam("transactionType", "1")
                        .queryParam("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.range").value("weekly"))
                .andExpect(jsonPath("$.result.transactionType").value(1))
                .andExpect(jsonPath("$.result.periodStart").value("2026-01-01"))
                .andExpect(jsonPath("$.result.periodEnd").value("2026-01-07"))
                .andExpect(jsonPath("$.result.total").value(123.0))
                .andExpect(jsonPath("$.result.series", hasSize(1)))
                .andExpect(jsonPath("$.result.categories", hasSize(1)))
                .andExpect(jsonPath("$.result.categories[0].categoryId").value(10))
                .andExpect(jsonPath("$.result.categories[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.result.categories[0].amount").value(123.0));
    }

    @Test
    public void categorySummaryHappyPathReturnsDto() throws Exception {
        mockMvc.perform(get("/api/stats/category-summary")
                        .header("Authorization", bearerToken)
                        .queryParam("range", "weekly")
                        .queryParam("transactionType", "1")
                        .queryParam("categoryId", "10")
                        .queryParam("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.range").value("weekly"))
                .andExpect(jsonPath("$.result.transactionType").value(1))
                .andExpect(jsonPath("$.result.categoryId").value(10))
                .andExpect(jsonPath("$.result.periodStart").value("2026-01-01"))
                .andExpect(jsonPath("$.result.periodEnd").value("2026-01-07"))
                .andExpect(jsonPath("$.result.total").value(50.0))
                .andExpect(jsonPath("$.result.series", hasSize(1)));
    }
}
