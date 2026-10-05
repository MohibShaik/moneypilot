package com.moneypilot.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 *
 * <p>Defines the API metadata and a global HTTP Bearer (JWT) security scheme so
 * Swagger UI shows an <b>Authorize</b> button: paste the {@code accessToken}
 * returned by {@code POST /api/v1/auth/login} and all protected endpoints are
 * called with {@code Authorization: Bearer <token>}.
 *
 * <p>The spec is served at {@code /v3/api-docs} and the UI at
 * {@code /swagger-ui.html} — both permitted without authentication in
 * {@link SecurityConfig}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI moneyPilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MoneyPilot API")
                        .version("v1")
                        .description("""
                                REST API for MoneyPilot, a personal finance app covering \
                                authentication, categories, payment methods, loans and EMIs, \
                                credit cards and statements, expenses, payments, and a \
                                dashboard summary.

                                All responses use the envelope \
                                `{ "success": boolean, "data": <payload>, "error": <ApiError> }`. \
                                Authenticate via `POST /api/v1/auth/login`, then click \
                                **Authorize** and supply the returned `accessToken`.""")
                        .contact(new Contact().name("MoneyPilot"))
                        .license(new License().name("Proprietary")))
                // Apply the bearer scheme globally; public endpoints simply
                // ignore the (optional) token.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token from POST /api/v1/auth/login")));
    }
}
