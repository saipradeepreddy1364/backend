package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "https://compiler-mu-two.vercel.app")
public class AIController {

    @Value("${ANTHROPIC_API_KEY}")
    private String anthropicApiKey;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody String body) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", anthropicApiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages",
                entity,
                String.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException e) {
            // Returns Anthropic's actual error message (auth failure, bad request etc.)
            return ResponseEntity.status(e.getStatusCode())
                .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
