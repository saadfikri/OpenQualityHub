package com.openqualityhub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Open Quality Hub API")
                .description("""
                    Open-source Quality Engineering & Test Management Platform.

                    Features:
                    - Test automation result import (JUnit XML, Playwright JSON)
                    - Quality metrics dashboard with pass rate trends and flaky test detection
                    - Cross-team QA service management with SLA tracking
                    - Documentation intelligence with health monitoring
                    - Release confidence scoring with quality gates and sign-off chains
                    - External integrations via webhooks (GitHub, GitLab, Jenkins, Jira)

                    **Authentication:** All endpoints except `/auth/login`, `/auth/refresh`, and `/webhooks/**` require a Bearer JWT token.
                    """)
                .version("0.1.0")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                .contact(new Contact().name("Open Quality Hub").url("https://github.com/openqualityhub"))
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT access token obtained from POST /api/v1/auth/login")
                )
            );
    }
}
