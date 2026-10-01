package com.ecommerce.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.ecommerce.TestcontainersConfig;
import com.ecommerce.users.domain.User;
import com.ecommerce.users.infrastructure.UserRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AuthApiIntegrationTest {

    private static final String PASSWORD = "Clave1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void registerCreatesCustomerWithoutExposingPassword() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email.toUpperCase(), PASSWORD)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/users/me"))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.roles").value(containsInAnyOrder("CUSTOMER")))
            .andExpect(jsonPath("$.permissions").isEmpty())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User stored = userRepository.findByEmail(email).orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void registerRejectsDuplicateEmailIgnoringCase() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email.toUpperCase(), PASSWORD)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe una cuenta con ese email"));
    }

    @Test
    void registerValidatesFieldsAndPasswordPolicy() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"no-es-email\",\"password\":\"\",\"firstName\":\"\",\"lastName\":\"Pérez\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("email", "password", "firstName")));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(uniqueEmail(), "soloLetras")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsAccessTokenThatAuthenticatesMe() throws Exception {
        String email = uniqueEmail();
        register(email);

        String token = login(email, PASSWORD);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.roles").value(containsInAnyOrder("CUSTOMER")));
    }

    @Test
    void loginFailsWithSameMessageForWrongPasswordUnknownEmailAndDisabledUser() throws Exception {
        String email = uniqueEmail();
        register(email);
        String disabledEmail = uniqueEmail();
        register(disabledEmail);
        transactionTemplate.executeWithoutResult(status ->
            userRepository.findByEmail(disabledEmail).orElseThrow().disable());

        for (String[] credentials : new String[][] {
                {email, "OtraClave999"}, {uniqueEmail(), PASSWORD}, {disabledEmail, PASSWORD}}) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(credentials[0], credentials[1])))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"));
        }
    }

    @Test
    void bootstrapAdminCanLogInWithAllPermissions() throws Exception {
        String token = login("admin@test.local", "AdminTest123");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles").value(containsInAnyOrder("ADMIN")))
            .andExpect(jsonPath("$.permissions").value(containsInAnyOrder(
                "backoffice:access", "users:read", "users:manage", "roles:read", "roles:manage")));
    }

    @Test
    void protectedEndpointsRejectMissingOrInvalidTokens() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email, PASSWORD);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tampered))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer no.es.un-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotGrantHimselfPermissionsByForgingTheToken() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email, PASSWORD);

        // Un token firmado con otra clave no es válido aunque declare permisos de ADMIN.
        String forged = ForgedTokens.signedWithWrongKey(JsonPath.read(decodePayload(token), "$.sub"));
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + forged))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.permissions").value(not(hasItem("users:manage"))));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email, PASSWORD)))
            .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String decodePayload(String jwt) {
        return new String(java.util.Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private static String registerJson(String email, String password) {
        return """
            {"email":"%s","password":"%s","firstName":"Ana","lastName":"Pérez"}
            """.formatted(email, password);
    }

    private static String loginJson(String email, String password) {
        return """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
    }
}
