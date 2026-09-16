package ru.yandex.practicum.oauth0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.oauth0.auth.AuthApp;
import ru.yandex.practicum.oauth0.rs.ResourceApp;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AuthApp.class, ResourceApp.class})
@AutoConfigureMockMvc
public class OAuth2SystemFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CLIENT_WEB_ID = "payments-web-app";
    private static final String CLIENT_WEB_SECRET = "payments-secret";

    private static final String CLIENT_SERVICE_ID = "billing-service";
    private static final String CLIENT_SERVICE_SECRET = "billing-secret";

    private static final String USER_REGULAR = "ivan_ivanov";
    private static final String USER_REGULAR_PASSWORD = "password123";

    private static final String MOCK_RS_SECRET = "wrong-and-invalid-secret-key-for-testing-purposes-only-32bytes";

    private String getAccessToken(String clientId, String clientSecret, String grantType, String username, String password) throws Exception {
        var request = post("/api/auth/token")
                .with(httpBasic(clientId, clientSecret))
                .param("grant_type", grantType)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);

        if (username != null) request.param("username", username);
        if (password != null) request.param("password", password);

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        if (result.getResponse().getStatus() != 200) {
            throw new RuntimeException("Auth failed! Status: " + result.getResponse().getStatus() +
                    "\nResponse: " + result.getResponse().getContentAsString());
        }

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());

        if (jsonNode.has("access_token")) {
            return jsonNode.get("access_token").asText();
        } else {
            throw new RuntimeException("No access_token in response: " + result.getResponse().getContentAsString());
        }
    }

    @Test
    public void testPasswordGrant_Success_And_RsGet200() throws Exception {
        String accessToken = getAccessToken(CLIENT_WEB_ID, CLIENT_WEB_SECRET, "password", USER_REGULAR, USER_REGULAR_PASSWORD);

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testPasswordGrant_InvalidPassword_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                        .with(httpBasic(CLIENT_WEB_ID, CLIENT_WEB_SECRET))
                        .param("grant_type", "password")
                        .param("username", USER_REGULAR)
                        .param("password", "WRONG_PASSWORD_ABC")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testClientCredentials_SuccessGet_But_ForbiddenPost() throws Exception {
        String accessToken = getAccessToken(CLIENT_SERVICE_ID, CLIENT_SERVICE_SECRET, "client_credentials", null, null);

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testRefreshTokenFlow_ReuseShouldFail() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .with(httpBasic(CLIENT_WEB_ID, CLIENT_WEB_SECRET))
                        .param("grant_type", "password")
                        .param("username", USER_REGULAR)
                        .param("password", USER_REGULAR_PASSWORD)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andReturn();

        String firstRefreshToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("refresh_token").asText();

        mockMvc.perform(post("/api/auth/token")
                        .with(httpBasic(CLIENT_WEB_ID, CLIENT_WEB_SECRET))
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", firstRefreshToken)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/token")
                        .with(httpBasic(CLIENT_WEB_ID, CLIENT_WEB_SECRET))
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", firstRefreshToken)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testRevocation_AccessToken_ShouldReturn401OnRS() throws Exception {
        String accessToken = getAccessToken(CLIENT_WEB_ID, CLIENT_WEB_SECRET, "password", USER_REGULAR, USER_REGULAR_PASSWORD);

        mockMvc.perform(post("/api/auth/revoke")
                        .with(httpBasic(CLIENT_WEB_ID, CLIENT_WEB_SECRET))
                        .param("token", accessToken)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testIntrospectionFlow() throws Exception {
        String accessToken = getAccessToken(CLIENT_WEB_ID, CLIENT_WEB_SECRET, "password", USER_REGULAR, USER_REGULAR_PASSWORD);

        mockMvc.perform(post("/api/auth/introspect")
                        .with(httpBasic(CLIENT_WEB_ID, CLIENT_WEB_SECRET))
                        .param("token", accessToken)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    public void testTamperedSignature_ShouldReturn401() throws Exception {
        String validToken = getAccessToken(CLIENT_WEB_ID, CLIENT_WEB_SECRET, "password", USER_REGULAR, USER_REGULAR_PASSWORD);

        String tamperedToken = validToken.substring(0, validToken.length() - 1) + "Z";

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testExpiredToken_ShouldReturn401() throws Exception {
        String expiredToken = Jwts.builder()
                .setSubject(USER_REGULAR)
                .setAudience("payments-api")
                .setExpiration(Date.from(Instant.now().minusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(MOCK_RS_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

}