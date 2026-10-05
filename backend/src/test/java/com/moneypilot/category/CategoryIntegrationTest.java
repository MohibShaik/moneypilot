package com.moneypilot.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * End-to-end tests for the Categories API against the configured local
 * PostgreSQL (no Docker/Testcontainers/H2). Drives the real HTTP stack,
 * Spring Security (JWT), and persistence. Verifies ownership isolation: a user
 * sees system + own categories and cannot read/modify another user's.
 *
 * <p>Created users are removed in cleanup; their categories cascade-delete with
 * them (V2 {@code categories.user_id ON DELETE CASCADE}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
        createdEmails.clear();
    }

    // ---- auth helpers ------------------------------------------------------

    private String uniqueEmail() {
        String email = "cat-it+" + UUID.randomUUID() + "@example.com";
        createdEmails.add(email);
        return email;
    }

    /** Registers a new user and logs in, returning a Bearer token. */
    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        String register = """
                {"email":"%s","password":"password1","displayName":"Cat Tester"}
                """.formatted(email);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(register))
                .andExpect(status().isCreated());

        String login = """
                {"email":"%s","password":"password1"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    private String createCategoryBody(String name, String type) {
        return """
                {"name":"%s","categoryType":"%s"}
                """.formatted(name, type);
    }

    private String idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    // ---- tests -------------------------------------------------------------

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void createListUpdateDeleteOwnedCategory() throws Exception {
        String token = registerAndLogin();
        String unique = "Cat-" + UUID.randomUUID();

        // Create
        MvcResult created = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCategoryBody(unique, "EXPENSE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(unique))
                .andExpect(jsonPath("$.data.categoryType").value("EXPENSE"))
                .andExpect(jsonPath("$.data.system").value(false))
                .andReturn();
        String id = idOf(created);

        // List includes the new category
        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + id + "')].name").value(unique));

        // Update
        String renamed = unique + "-renamed";
        mockMvc.perform(put("/api/v1/categories/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCategoryBody(renamed, "INCOME")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(renamed))
                .andExpect(jsonPath("$.data.categoryType").value("INCOME"));

        // Delete
        mockMvc.perform(delete("/api/v1/categories/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsDuplicateNameForSameUser() throws Exception {
        String token = registerAndLogin();
        String name = "Dup-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCategoryBody(name, "EXPENSE")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCategoryBody(name, "EXPENSE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CATEGORY"));
    }

    @Test
    void validationFailsForBadRequest() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"categoryType\":\"EXPENSE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cannotModifyAnotherUsersCategory() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();

        // User A creates a category.
        MvcResult created = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCategoryBody("Owned-" + UUID.randomUUID(), "EXPENSE")))
                .andExpect(status().isCreated())
                .andReturn();
        String id = idOf(created);

        // User B cannot update it (reported as not found, not forbidden).
        mockMvc.perform(put("/api/v1/categories/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCategoryBody("Hijack", "EXPENSE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));

        // User B cannot delete it either.
        mockMvc.perform(delete("/api/v1/categories/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // User B's list does not contain A's category.
        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + id + "')]").doesNotExist());
    }
}
