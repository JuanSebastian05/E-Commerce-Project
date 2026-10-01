package com.ecommerce.users.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.ecommerce.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class UserAdministrationApiIntegrationTest {

    private static final String PASSWORD = "Clave1234";

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        adminToken = accessTokenOf(login("admin@test.local", "AdminTest123"));
    }

    @Test
    void endpointsRequireTheRightPermission() throws Exception {
        String customerToken = accessTokenOf(login(registerCustomer(), PASSWORD));
        String supportToken = accessTokenOf(login(createUser("SUPPORT"), PASSWORD));

        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(customerToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("No tienes permiso para esta acción"));

        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(supportToken)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", bearer(supportToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson(uniqueEmail(), "ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotEscalateHisOwnPrivileges() throws Exception {
        String email = registerCustomer();
        String customerToken = accessTokenOf(login(email, PASSWORD));
        String customerId = JsonPath.read(mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", bearer(customerToken)))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/v1/users/{id}/roles", customerId)
                .header("Authorization", bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"ADMIN\"]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesUserWhoCanLogInWithThoseRoles() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson(email, "support", "WAREHOUSE")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/api/v1/users/")))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.roles").value(containsInAnyOrder("SUPPORT", "WAREHOUSE")))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String token = accessTokenOf(login(email, PASSWORD));
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(token)))
            .andExpect(jsonPath("$.permissions").value(containsInAnyOrder("backoffice:access", "users:read")));
    }

    @Test
    void createValidatesRolesEmailAndPassword() throws Exception {
        String email = createUser("SUPPORT");

        mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson(uniqueEmail(), "SUPPORT", "JEFE")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Roles inexistentes: JEFE"));
        mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson(email, "SUPPORT")))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"firstName\":\"A\",\"lastName\":\"B\",\"roles\":[]}"
                    .formatted(uniqueEmail(), PASSWORD)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listFiltersByEmailRoleAndStatusAndValidatesPageSize() throws Exception {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        String warehouseEmail = createUserWithEmail("lista-" + marker + "-a@example.com", "WAREHOUSE");
        createUserWithEmail("lista-" + marker + "-b@example.com", "SUPPORT");

        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken))
                .param("email", marker.toUpperCase()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken))
                .param("email", marker).param("role", "warehouse"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].email").value(warehouseEmail));

        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken))
                .param("enabled", "true").param("size", "5"))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.content[*].enabled").value(everyItem(org.hamcrest.Matchers.is(true))));

        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken)).param("size", "500"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsUserOr404() throws Exception {
        String id = idOf(createUser("SUPPORT"));

        mockMvc.perform(get("/api/v1/users/{id}", id).header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id));
        mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()).header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    void disablingRevokesSessionsAndBlocksLoginUntilReenabled() throws Exception {
        String email = createUser("SUPPORT");
        String id = idOf(email);
        String refreshToken = refreshTokenOf(login(email, PASSWORD));

        changeStatus(id, false).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));

        refresh(refreshToken).andExpect(status().isUnauthorized());
        loginRequest(email, PASSWORD).andExpect(status().isUnauthorized());

        changeStatus(id, true).andExpect(status().isOk());
        loginRequest(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void changingRolesRevokesSessionsAndUpdatesPermissions() throws Exception {
        String email = createUser("SUPPORT");
        String id = idOf(email);
        String refreshToken = refreshTokenOf(login(email, PASSWORD));

        mockMvc.perform(put("/api/v1/users/{id}/roles", id).header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"WAREHOUSE\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles").value(containsInAnyOrder("WAREHOUSE")));

        refresh(refreshToken).andExpect(status().isUnauthorized());
        String token = accessTokenOf(login(email, PASSWORD));
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotDisableHimselfNorDropHisOwnUserManagement() throws Exception {
        String adminEmail = createUser("ADMIN");
        String adminId = idOf(adminEmail);
        String token = accessTokenOf(login(adminEmail, PASSWORD));

        mockMvc.perform(patch("/api/v1/users/{id}/status", adminId).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("No puedes desactivar tu propia cuenta"));

        mockMvc.perform(put("/api/v1/users/{id}/roles", adminId).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"SUPPORT\"]}"))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/users/{id}", adminId).header("Authorization", bearer(token)))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.roles").value(hasItem("ADMIN")));
    }

    private ResultActions changeStatus(String id, boolean enabled) throws Exception {
        return mockMvc.perform(patch("/api/v1/users/{id}/status", id).header("Authorization", bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"enabled\":" + enabled + "}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", refreshToken)));
    }

    private String createUser(String... roles) throws Exception {
        return createUserWithEmail(uniqueEmail(), roles);
    }

    private String createUserWithEmail(String email, String... roles) throws Exception {
        mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson(email, roles)))
            .andExpect(status().isCreated());
        return email;
    }

    private String idOf(String email) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken))
                .param("email", email))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.content[0].id");
    }

    private String registerCustomer() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"firstName\":\"Ana\",\"lastName\":\"Pérez\"}"
                    .formatted(email, PASSWORD)))
            .andExpect(status().isCreated());
        return email;
    }

    private ResultActions loginRequest(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private MvcResult login(String email, String password) throws Exception {
        return loginRequest(email, password).andExpect(status().isOk()).andReturn();
    }

    private static String accessTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String refreshTokenOf(MvcResult result) {
        return result.getResponse().getCookie("refresh_token").getValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "admin-api-" + UUID.randomUUID() + "@example.com";
    }

    private static String createUserJson(String email, String... roles) {
        String roleList = String.join("\",\"", roles);
        return """
            {"email":"%s","password":"%s","firstName":"Luis","lastName":"Gómez","roles":["%s"]}
            """.formatted(email, PASSWORD, roleList);
    }
}
