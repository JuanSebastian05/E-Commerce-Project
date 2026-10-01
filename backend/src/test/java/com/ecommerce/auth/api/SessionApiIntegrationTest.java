package com.ecommerce.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.ecommerce.TestcontainersConfig;
import com.ecommerce.users.infrastructure.UserRepository;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class SessionApiIntegrationTest {

    private static final String PASSWORD = "Clave1234";
    private static final String COOKIE = "refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void loginSetsSecureHttpOnlyRefreshCookieScopedToAuth() throws Exception {
        String email = registerNewUser();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(header().string("Set-Cookie", containsString(COOKIE + "=")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", containsString("Secure")))
            .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
            .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
            .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void refreshRotatesTokenAndReturnsWorkingAccessToken() throws Exception {
        String email = registerNewUser();
        String firstRefresh = refreshCookieOf(login(email));

        MvcResult refreshed = refresh(firstRefresh).andExpect(status().isOk()).andReturn();
        String secondRefresh = refreshCookieOf(refreshed);
        String accessToken = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.accessToken");

        assertThat(secondRefresh).isNotBlank().isNotEqualTo(firstRefresh);
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeSession() throws Exception {
        String email = registerNewUser();
        String firstRefresh = refreshCookieOf(login(email));
        String secondRefresh = refreshCookieOf(refresh(firstRefresh).andExpect(status().isOk()).andReturn());

        refresh(firstRefresh).andExpect(status().isUnauthorized());

        // El token legítimo más reciente también queda revocado.
        refresh(secondRefresh).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutOrWithUnknownCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
        refresh("token-inventado").andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheSessionAndClearsTheCookie() throws Exception {
        String email = registerNewUser();
        String refreshToken = refreshCookieOf(login(email));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(COOKIE, refreshToken)))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUserCannotRefresh() throws Exception {
        String email = registerNewUser();
        String refreshToken = refreshCookieOf(login(email));
        transactionTemplate.executeWithoutResult(tx -> userRepository.findByEmail(email).orElseThrow().disable());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsBlockedAfterFiveFailuresEvenWithTheRightPassword() throws Exception {
        String email = registerNewUser();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, "Incorrecta" + i)))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, PASSWORD)))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.status").value(429));
    }

    private String registerNewUser() throws Exception {
        String email = "session-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s","firstName":"Ana","lastName":"Pérez"}
                    """.formatted(email, PASSWORD)))
            .andExpect(status().isCreated());
        return email;
    }

    private MvcResult login(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(COOKIE, refreshToken)));
    }

    private static String refreshCookieOf(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(COOKIE);
        assertThat(cookie).as("cookie %s en la respuesta", COOKIE).isNotNull();
        return cookie.getValue();
    }

    private static String loginJson(String email, String password) {
        return """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
    }
}
