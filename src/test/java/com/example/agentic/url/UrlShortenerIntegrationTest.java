package com.example.agentic.url;

import com.example.agentic.AbstractIntegrationTest;
import com.example.agentic.url.api.AnalyticsResponse;
import com.example.agentic.url.api.CreateUrlRequest;
import com.example.agentic.url.api.UrlResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class UrlShortenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void createRedirectAndTrackAnalyticsEndToEnd() {
        ResponseEntity<UrlResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/urls", new CreateUrlRequest("https://example.com/long/path", null), UrlResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UrlResponse created = createResponse.getBody();
        assertThat(created).isNotNull();
        assertThat(created.shortCode()).hasSize(7);
        assertThat(created.status()).isEqualTo("ACTIVE");

        SimpleClientHttpRequestFactory noRedirectsFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        TestRestTemplate noRedirectTemplate = new TestRestTemplate();
        noRedirectTemplate.getRestTemplate().setRequestFactory(noRedirectsFactory);
        ResponseEntity<String> redirectResponse = noRedirectTemplate.getForEntity(
                "http://localhost:" + port + "/" + created.shortCode(), String.class);
        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirectResponse.getHeaders().getLocation()).hasToString("https://example.com/long/path");

        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            ResponseEntity<AnalyticsResponse> analytics = restTemplate.getForEntity(
                    "/api/v1/urls/" + created.shortCode() + "/analytics", AnalyticsResponse.class);
            assertThat(analytics.getBody()).isNotNull();
            assertThat(analytics.getBody().totalClicks()).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void rejectsDisallowedScheme() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/urls", new CreateUrlRequest("javascript:alert(1)", null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void returnsNotFoundForUnknownShortCode() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/urls/doesnotexist", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
