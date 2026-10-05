package com.moneypilot.paymentmethod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneypilot.creditcard.CardStatementRepository;
import com.moneypilot.creditcard.CreditCard;
import com.moneypilot.creditcard.CreditCardRepository;
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
 * End-to-end tests for the Payment Methods API against the configured local
 * PostgreSQL (no Docker/Testcontainers/H2). Drives the real HTTP stack, Spring
 * Security (JWT), and persistence. Verifies CRUD, ownership isolation,
 * credit-card linkage (owned card required), and validation.
 *
 * <p>Cleanup respects the RESTRICT FKs: a user's payment methods are deleted
 * first (they reference credit cards), then the user's credit cards (and any
 * statements), then the user.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentMethodIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private CardStatementRepository cardStatementRepository;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent((User u) -> {
                // RESTRICT FKs: payment methods (reference cards) first, then
                // the cards' statements and the cards, then the user.
                paymentMethodRepository.deleteAll(
                        paymentMethodRepository.findByUserIdOrderByLabelAsc(u.getId()));
                for (CreditCard card : creditCardRepository.findByUserIdOrderByCreatedAtDesc(u.getId())) {
                    cardStatementRepository.deleteByCreditCardId(card.getId());
                }
                creditCardRepository.deleteAll(
                        creditCardRepository.findByUserIdOrderByCreatedAtDesc(u.getId()));
                userRepository.delete(u);
            });
        }
        createdEmails.clear();
    }

    private String uniqueEmail() {
        String email = "pm-it+" + UUID.randomUUID() + "@example.com";
        createdEmails.add(email);
        return email;
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1","displayName":"PM Tester"}
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

    private String body(String type, String label) {
        return """
                {"type":"%s","label":"%s"}
                """.formatted(type, label);
    }

    private String idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/payment-methods"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void createListUpdateDeleteOwned() throws Exception {
        String token = registerAndLogin();

        MvcResult created = mockMvc.perform(post("/api/v1/payment-methods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("UPI", "My UPI")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("UPI"))
                .andExpect(jsonPath("$.data.label").value("My UPI"))
                .andExpect(jsonPath("$.data.creditCardId").doesNotExist())
                .andReturn();
        String id = idOf(created);

        mockMvc.perform(get("/api/v1/payment-methods").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + id + "')].label").value("My UPI"));

        mockMvc.perform(put("/api/v1/payment-methods/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CASH", "Wallet cash")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("CASH"))
                .andExpect(jsonPath("$.data.label").value("Wallet cash"));

        mockMvc.perform(delete("/api/v1/payment-methods/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void creditCardWithoutCardIdIsBadRequest() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(post("/api/v1/payment-methods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CREDIT_CARD", "HDFC Regalia")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void creditCardWithNonOwnedCardIsNotFound() throws Exception {
        String token = registerAndLogin();
        String randomCardId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/payment-methods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"CREDIT_CARD","label":"HDFC","creditCardId":"%s"}
                                """.formatted(randomCardId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CREDIT_CARD_NOT_FOUND"));
    }

    @Test
    void creditCardWithOwnedCardIsCreated() throws Exception {
        String token = registerAndLogin();

        // Create an owned credit card first.
        MvcResult card = mockMvc.perform(post("/api/v1/credit-cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardName":"Regalia","bank":"HDFC","lastFour":"1234",
                                 "creditLimit":200000.00,"statementDay":5,"paymentDueDay":25}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String cardId = idOf(card);

        mockMvc.perform(post("/api/v1/payment-methods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"CREDIT_CARD","label":"HDFC Regalia","creditCardId":"%s"}
                                """.formatted(cardId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.data.creditCardId").value(cardId));
    }

    @Test
    void validationFailsForBlankLabel() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(post("/api/v1/payment-methods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"UPI\",\"label\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cannotModifyAnotherUsersPaymentMethod() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();

        MvcResult created = mockMvc.perform(post("/api/v1/payment-methods")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("UPI", "A's UPI")))
                .andExpect(status().isCreated())
                .andReturn();
        String id = idOf(created);

        mockMvc.perform(put("/api/v1/payment-methods/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CASH", "Hijack")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_METHOD_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/payment-methods/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/payment-methods").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + id + "')]").doesNotExist());
    }
}
