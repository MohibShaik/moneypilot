package com.moneypilot.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moneypilot.auth.security.JwtAuthenticationFilter;
import com.moneypilot.auth.security.JwtService;
import com.moneypilot.auth.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test for the health endpoint. Verifies the response status, content
 * type, and the ApiResponse JSON shape without loading the full application
 * context or a database.
 *
 * <p>The security filter chain beans are imported and {@link JwtService} is
 * mocked so the stateless {@link SecurityConfig} can be constructed in the
 * slice. Health is a public endpoint, so no token is needed.
 */
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @Test
    void healthReturnsUpEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.application").value("MoneyPilot"))
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.error").doesNotExist());
    }
}
