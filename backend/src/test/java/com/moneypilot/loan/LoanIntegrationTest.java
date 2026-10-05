package com.moneypilot.loan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneypilot.user.User;
import com.moneypilot.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end tests for the Loans API against the configured local PostgreSQL
 * (no Docker/Testcontainers/H2). Drives the real HTTP stack, Spring Security
 * (JWT), and persistence. Verifies CRUD, server-side EMI schedule generation
 * (persisted rows prove the DB component-sum CHECK is satisfied), ownership
 * isolation, and validation.
 *
 * <p>Cleanup: {@code loan_emis.loan_id} and {@code loans.user_id} are both ON
 * DELETE RESTRICT, so EMIs are deleted first, then loans, then the user.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanEmiRepository emiRepository;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent((User u) -> {
                // RESTRICT FKs: delete EMIs, then loans, then the user.
                for (Loan loan : loanRepository.findByUserIdOrderByCreatedAtDesc(u.getId())) {
                    emiRepository.deleteByLoanId(loan.getId());
                }
                loanRepository.deleteAll(loanRepository.findByUserIdOrderByCreatedAtDesc(u.getId()));
                userRepository.delete(u);
            });
        }
        createdEmails.clear();
    }

    private String uniqueEmail() {
        String email = "loan-it+" + UUID.randomUUID() + "@example.com";
        createdEmails.add(email);
        return email;
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1","displayName":"Loan Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String createLoanBody() {
        return """
                {"name":"Home Loan","lender":"HDFC","loanType":"HOME",
                 "principalAmount":100000.00,"interestRate":12.000,
                 "startDate":"2026-01-01","tenureMonths":12}
                """;
    }

    private String idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/loans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void createGeneratesScheduleAndDetailReturnsIt() throws Exception {
        String token = registerAndLogin();

        MvcResult created = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoanBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Home Loan"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.outstandingPrincipal").value(100000.00))
                .andExpect(jsonPath("$.data.endDate").value("2027-01-01"))
                .andExpect(jsonPath("$.data.emis.length()").value(12))
                .andReturn();

        // The schedule persisted successfully, which proves every row satisfied
        // the DB CHECK principal_component + interest_component = emi_amount.
        JsonNode emis = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("emis");
        for (JsonNode emi : emis) {
            double principal = emi.path("principalComponent").asDouble();
            double interest = emi.path("interestComponent").asDouble();
            double amount = emi.path("emiAmount").asDouble();
            org.assertj.core.api.Assertions.assertThat(principal + interest)
                    .isCloseTo(amount, org.assertj.core.api.Assertions.within(0.0001));
        }

        String id = idOf(created);
        mockMvc.perform(get("/api/v1/loans/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emis.length()").value(12))
                .andExpect(jsonPath("$.data.emis[0].installmentNo").value(1))
                .andExpect(jsonPath("$.data.emis[0].status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.emis[0].dueDate").value("2026-02-01"));
    }

    @Test
    void listOmitsEmis() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoanBody()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/loans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Home Loan"))
                .andExpect(jsonPath("$.data[0].emis").doesNotExist());
    }

    @Test
    void updateChangesNameLenderStatus() throws Exception {
        String token = registerAndLogin();
        MvcResult created = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoanBody()))
                .andExpect(status().isCreated())
                .andReturn();
        String id = idOf(created);

        mockMvc.perform(put("/api/v1/loans/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed Loan","lender":"ICICI","status":"CLOSED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed Loan"))
                .andExpect(jsonPath("$.data.lender").value("ICICI"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                // Financial terms unchanged.
                .andExpect(jsonPath("$.data.principalAmount").value(100000.00))
                .andExpect(jsonPath("$.data.tenureMonths").value(12));
    }

    @Test
    void deleteRemovesLoanAndEmis() throws Exception {
        String token = registerAndLogin();
        MvcResult created = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoanBody()))
                .andExpect(status().isCreated())
                .andReturn();
        String id = idOf(created);

        mockMvc.perform(delete("/api/v1/loans/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/loans/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LOAN_NOT_FOUND"));
    }

    @Test
    void validationFailsForBadPrincipalAndTenure() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bad","loanType":"HOME","principalAmount":-5,
                                 "interestRate":10.0,"startDate":"2026-01-01","tenureMonths":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cannotAccessAnotherUsersLoan() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();

        MvcResult created = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoanBody()))
                .andExpect(status().isCreated())
                .andReturn();
        String id = idOf(created);

        mockMvc.perform(get("/api/v1/loans/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LOAN_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/loans/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hijack","status":"CLOSED"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/loans/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
