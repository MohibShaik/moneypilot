package com.moneypilot.creditcard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * End-to-end tests for the Credit Cards + Card Statements API against the
 * configured local PostgreSQL. Drives the real HTTP stack, Spring Security
 * (JWT), and persistence. Verifies card CRUD, statement CRUD with derived
 * status (persisted rows prove the DB CHECKs), duplicate-date 409, cross-field
 * 400, and ownership isolation.
 *
 * <p>Cleanup respects the RESTRICT FKs: statements first, then cards, then the
 * user.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CreditCardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CreditCardRepository cardRepository;

    @Autowired
    private CardStatementRepository statementRepository;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent((User u) -> {
                for (CreditCard card : cardRepository.findByUserIdOrderByCreatedAtDesc(u.getId())) {
                    statementRepository.deleteByCreditCardId(card.getId());
                }
                cardRepository.deleteAll(cardRepository.findByUserIdOrderByCreatedAtDesc(u.getId()));
                userRepository.delete(u);
            });
        }
        createdEmails.clear();
    }

    private String uniqueEmail() {
        String email = "cc-it+" + UUID.randomUUID() + "@example.com";
        createdEmails.add(email);
        return email;
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1","displayName":"CC Tester"}
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

    private String cardBody() {
        return """
                {"cardName":"Regalia","bank":"HDFC","lastFour":"1234",
                 "creditLimit":200000.00,"statementDay":5,"paymentDueDay":25}
                """;
    }

    private String idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String createCard(String token) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/credit-cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardBody()))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(created);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/credit-cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void cardCreateGetUpdateDelete() throws Exception {
        String token = registerAndLogin();

        MvcResult created = mockMvc.perform(post("/api/v1/credit-cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cardName").value("Regalia"))
                .andExpect(jsonPath("$.data.lastFour").value("1234"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        String id = idOf(created);

        mockMvc.perform(get("/api/v1/credit-cards/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));

        mockMvc.perform(put("/api/v1/credit-cards/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardName":"Regalia Gold","bank":"HDFC","creditLimit":250000.00,
                                 "statementDay":6,"paymentDueDay":26,"status":"BLOCKED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardName").value("Regalia Gold"))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                // lastFour immutable.
                .andExpect(jsonPath("$.data.lastFour").value("1234"));

        mockMvc.perform(delete("/api/v1/credit-cards/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/credit-cards/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CREDIT_CARD_NOT_FOUND"));
    }

    @Test
    void cardValidationFailsForBadLastFour() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(post("/api/v1/credit-cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardName":"Bad","bank":"HDFC","lastFour":"12",
                                 "creditLimit":200000.00,"statementDay":5,"paymentDueDay":25}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void statementCreateDerivesStatusAndListsAndDeletes() throws Exception {
        String token = registerAndLogin();
        String cardId = createCard(token);
        String base = "/api/v1/credit-cards/" + cardId + "/statements";

        MvcResult created = mockMvc.perform(post(base)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statementDate":"2026-01-05","dueDate":"2026-01-25",
                                 "statementAmount":1000.00,"minimumDue":100.00,"paidAmount":400.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_PAID"))
                .andReturn();
        String stmtId = idOf(created);

        mockMvc.perform(get(base).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + stmtId + "')].status").value("PARTIALLY_PAID"));

        // Mark fully paid via update -> derived PAID.
        mockMvc.perform(put(base + "/" + stmtId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dueDate":"2026-01-25","statementAmount":1000.00,
                                 "minimumDue":100.00,"paidAmount":1000.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(delete(base + "/" + stmtId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void duplicateStatementDateIsConflict() throws Exception {
        String token = registerAndLogin();
        String cardId = createCard(token);
        String base = "/api/v1/credit-cards/" + cardId + "/statements";
        String body = """
                {"statementDate":"2026-02-05","dueDate":"2026-02-25",
                 "statementAmount":500.00,"minimumDue":50.00}
                """;

        mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CARD_STATEMENT"));
    }

    @Test
    void statementCrossFieldValidationIsBadRequest() throws Exception {
        String token = registerAndLogin();
        String cardId = createCard(token);
        String base = "/api/v1/credit-cards/" + cardId + "/statements";

        // dueDate before statementDate.
        mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statementDate":"2026-03-10","dueDate":"2026-03-01",
                                 "statementAmount":1000.00,"minimumDue":100.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void cannotAccessAnotherUsersCardOrStatements() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        String cardId = createCard(tokenA);

        mockMvc.perform(get("/api/v1/credit-cards/" + cardId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CREDIT_CARD_NOT_FOUND"));

        // Statements of A's card are not reachable by B (parent card not owned).
        mockMvc.perform(get("/api/v1/credit-cards/" + cardId + "/statements")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CREDIT_CARD_NOT_FOUND"));
    }
}
