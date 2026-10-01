package com.ecommerce.users.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.ecommerce.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class RoleApiIntegrationTest {

    private static final String PASSWORD = "Clave1234";

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        adminToken = login("admin@test.local", "AdminTest123");
    }

    @Test
    void readingRequiresRolesReadAndManagingRequiresRolesManage() throws Exception {
        String supportToken = login(createUser("SUPPORT"), PASSWORD);

        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/roles").header("Authorization", bearer(supportToken)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/permissions").header("Authorization", bearer(supportToken)))
            .andExpect(status().isForbidden());
        createRole(supportToken, "INTRUSO_" + suffix(), List.of("users:manage"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listsRolesAndPermissionCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/roles").header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name").value(hasItems("ADMIN", "CUSTOMER", "SUPPORT", "WAREHOUSE")))
            .andExpect(jsonPath("$[?(@.name == 'SUPPORT')].permissions[*]")
                .value(containsInAnyOrder("backoffice:access", "users:read")));

        mockMvc.perform(get("/api/v1/permissions").header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].code").value(hasItems(
                "backoffice:access", "users:read", "users:manage", "roles:read", "roles:manage")));
    }

    @Test
    void customRoleGivesItsPermissionsToItsUsers() throws Exception {
        String name = "auditor_" + suffix().toLowerCase();

        String body = createRole(adminToken, name, List.of("backoffice:access", "users:read"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/api/v1/roles/")))
            .andExpect(jsonPath("$.name").value(name.toUpperCase()))
            .andExpect(jsonPath("$.system").value(false))
            .andReturn().getResponse().getContentAsString();

        String auditorToken = login(createUser(name.toUpperCase()), PASSWORD);
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(auditorToken)))
            .andExpect(status().isOk());

        String roleId = JsonPath.read(body, "$.id");
        replacePermissions(roleId, List.of("backoffice:access"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions").value(containsInAnyOrder("backoffice:access")));
    }

    @Test
    void createValidatesNameUniquenessAndPermissionCodes() throws Exception {
        createRole(adminToken, "SUPPORT", List.of()).andExpect(status().isConflict());
        createRole(adminToken, "con espacios", List.of()).andExpect(status().isBadRequest());
        createRole(adminToken, "NUEVO_" + suffix(), List.of("users:read", "catalog:fly"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Permisos inexistentes: catalog:fly"));
    }

    @Test
    void adminRoleKeepsItsAdministrationPermissions() throws Exception {
        replacePermissions(roleId("ADMIN"), List.of("backoffice:access", "users:read", "users:manage"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("El rol ADMIN debe conservar los permisos users:manage y roles:manage"));

        mockMvc.perform(get("/api/v1/roles").header("Authorization", bearer(adminToken)))
            .andExpect(jsonPath("$[?(@.name == 'ADMIN')].permissions[*]").value(hasItems("roles:manage")));
    }

    @Test
    void deleteRejectsSystemRolesAndRolesInUse() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/{id}", roleId("CUSTOMER")).header("Authorization", bearer(adminToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Los roles de sistema no se pueden borrar"));

        String usedName = "USADO_" + suffix();
        createRole(adminToken, usedName, List.of("backoffice:access")).andExpect(status().isCreated());
        createUser(usedName);
        mockMvc.perform(delete("/api/v1/roles/{id}", roleId(usedName)).header("Authorization", bearer(adminToken)))
            .andExpect(status().isConflict());

        String freeName = "LIBRE_" + suffix();
        createRole(adminToken, freeName, List.of()).andExpect(status().isCreated());
        String freeId = roleId(freeName);
        mockMvc.perform(delete("/api/v1/roles/{id}", freeId).header("Authorization", bearer(adminToken)))
            .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/roles/{id}", freeId).header("Authorization", bearer(adminToken)))
            .andExpect(status().isNotFound());
    }

    private ResultActions createRole(String token, String name, List<String> permissions) throws Exception {
        return mockMvc.perform(post("/api/v1/roles").header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"%s\",\"description\":\"Prueba\",\"permissions\":%s}"
                .formatted(name, jsonArray(permissions))));
    }

    private ResultActions replacePermissions(String roleId, List<String> permissions) throws Exception {
        return mockMvc.perform(put("/api/v1/roles/{id}/permissions", roleId).header("Authorization", bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"permissions\":%s}".formatted(jsonArray(permissions))));
    }

    private String roleId(String name) throws Exception {
        String body = mockMvc.perform(get("/api/v1/roles").header("Authorization", bearer(adminToken)))
            .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.name == '" + name + "')].id");
        return ids.getFirst();
    }

    private String createUser(String role) throws Exception {
        String email = "roles-api-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"firstName\":\"Eva\",\"lastName\":\"Ruiz\",\"roles\":[\"%s\"]}"
                    .formatted(email, PASSWORD, role)))
            .andExpect(status().isCreated());
        return email;
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String jsonArray(List<String> values) {
        return values.isEmpty() ? "[]" : "[\"" + String.join("\",\"", values) + "\"]";
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
