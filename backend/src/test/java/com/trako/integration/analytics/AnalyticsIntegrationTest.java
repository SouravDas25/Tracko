package com.trako.integration.analytics;

import com.trako.dtos.AnalyticsResponseDTO;
import com.trako.dtos.NamedSeriesDTO;
import com.trako.dtos.StatsPointDTO;
import com.trako.enums.TransactionType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.services.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AnalyticsIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private AnalyticsService analyticsService;

    private String bearerToken;

    @BeforeEach
    public void setup() {
        var user = createUniqueUser("AnalyticsUser");
        bearerToken = generateBearerToken(user);
    }

    @Test
    public void chartWithoutGroupByReturnsSingleAllSeries() throws Exception {
        AnalyticsResponseDTO dto = new AnalyticsResponseDTO(
                "monthly",
                TransactionType.DEBIT,
                "2026-01-01",
                "2026-01-31",
                500.0,
                List.of(new NamedSeriesDTO("All", List.of(new StatsPointDTO("Jan 2026", 500.0))))
        );
        when(analyticsService.getChartData(anyString(), any(TransactionType.class),
                any(Date.class), any(Date.class), any(), any(), any(), any()))
                .thenReturn(dto);

        mockMvc.perform(get("/api/analytics/chart")
                        .header("Authorization", bearerToken)
                        .queryParam("transactionType", "1")
                        .queryParam("startDate", "2026-01-01")
                        .queryParam("endDate", "2026-01-31")
                        .queryParam("granularity", "monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.granularity").value("monthly"))
                .andExpect(jsonPath("$.result.transactionType").value(1))
                .andExpect(jsonPath("$.result.total").value(500.0))
                .andExpect(jsonPath("$.result.groupedSeries", hasSize(1)))
                .andExpect(jsonPath("$.result.groupedSeries[0].name").value("All"))
                .andExpect(jsonPath("$.result.groupedSeries[0].series", hasSize(1)))
                .andExpect(jsonPath("$.result.groupedSeries[0].series[0].label").value("Jan 2026"))
                .andExpect(jsonPath("$.result.groupedSeries[0].series[0].value").value(500.0));
    }

    @Test
    public void chartWithGroupByCategoryReturnsPerCategorySeries() throws Exception {
        AnalyticsResponseDTO dto = new AnalyticsResponseDTO(
                "monthly",
                TransactionType.DEBIT,
                "2026-01-01",
                "2026-01-31",
                700.0,
                List.of(
                        new NamedSeriesDTO("Food", List.of(new StatsPointDTO("Jan 2026", 400.0))),
                        new NamedSeriesDTO("Transport", List.of(new StatsPointDTO("Jan 2026", 300.0)))
                )
        );
        when(analyticsService.getChartData(anyString(), any(TransactionType.class),
                any(Date.class), any(Date.class), any(), any(), any(), any()))
                .thenReturn(dto);

        mockMvc.perform(get("/api/analytics/chart")
                        .header("Authorization", bearerToken)
                        .queryParam("transactionType", "1")
                        .queryParam("startDate", "2026-01-01")
                        .queryParam("endDate", "2026-01-31")
                        .queryParam("granularity", "monthly")
                        .queryParam("groupBy", "category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.groupedSeries", hasSize(2)))
                .andExpect(jsonPath("$.result.groupedSeries[0].name").value("Food"))
                .andExpect(jsonPath("$.result.groupedSeries[0].series[0].value").value(400.0))
                .andExpect(jsonPath("$.result.groupedSeries[1].name").value("Transport"))
                .andExpect(jsonPath("$.result.groupedSeries[1].series[0].value").value(300.0))
                .andExpect(jsonPath("$.result.total").value(700.0));
    }

    @Test
    public void chartWithGroupByAccountReturnsPerAccountSeries() throws Exception {
        AnalyticsResponseDTO dto = new AnalyticsResponseDTO(
                "monthly",
                TransactionType.DEBIT,
                "2026-01-01",
                "2026-01-31",
                900.0,
                List.of(
                        new NamedSeriesDTO("Savings", List.of(new StatsPointDTO("Jan 2026", 600.0))),
                        new NamedSeriesDTO("Checking", List.of(new StatsPointDTO("Jan 2026", 300.0)))
                )
        );
        when(analyticsService.getChartData(anyString(), any(TransactionType.class),
                any(Date.class), any(Date.class), any(), any(), any(), any()))
                .thenReturn(dto);

        mockMvc.perform(get("/api/analytics/chart")
                        .header("Authorization", bearerToken)
                        .queryParam("transactionType", "1")
                        .queryParam("startDate", "2026-01-01")
                        .queryParam("endDate", "2026-01-31")
                        .queryParam("granularity", "monthly")
                        .queryParam("groupBy", "account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.groupedSeries", hasSize(2)))
                .andExpect(jsonPath("$.result.groupedSeries[0].name").value("Savings"))
                .andExpect(jsonPath("$.result.groupedSeries[0].series[0].value").value(600.0))
                .andExpect(jsonPath("$.result.groupedSeries[1].name").value("Checking"))
                .andExpect(jsonPath("$.result.groupedSeries[1].series[0].value").value(300.0))
                .andExpect(jsonPath("$.result.total").value(900.0));
    }

    @Test
    public void chartWithInvalidGroupByReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/analytics/chart")
                        .header("Authorization", bearerToken)
                        .queryParam("transactionType", "1")
                        .queryParam("startDate", "2026-01-01")
                        .queryParam("endDate", "2026-01-31")
                        .queryParam("groupBy", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid groupBy. Use category or account"));
    }

    @Test
    public void chartWithInvalidGranularityReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/analytics/chart")
                        .header("Authorization", bearerToken)
                        .queryParam("transactionType", "1")
                        .queryParam("startDate", "2026-01-01")
                        .queryParam("endDate", "2026-01-31")
                        .queryParam("granularity", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid granularity. Use weekly, monthly, or yearly"));
    }

    @Test
    public void chartWithoutGranularityDefaultsToMonthly() throws Exception {
        AnalyticsResponseDTO dto = new AnalyticsResponseDTO(
                "monthly",
                TransactionType.DEBIT,
                "2026-01-01",
                "2026-01-31",
                250.0,
                List.of(new NamedSeriesDTO("All", List.of(new StatsPointDTO("Jan 2026", 250.0))))
        );
        when(analyticsService.getChartData(anyString(), any(TransactionType.class),
                any(Date.class), any(Date.class), any(), any(), any(), any()))
                .thenReturn(dto);

        mockMvc.perform(get("/api/analytics/chart")
                        .header("Authorization", bearerToken)
                        .queryParam("transactionType", "1")
                        .queryParam("startDate", "2026-01-01")
                        .queryParam("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.granularity").value("monthly"));
    }
}
