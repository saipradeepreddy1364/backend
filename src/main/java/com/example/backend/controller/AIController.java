package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") // Allows your frontend to talk to your backend
public class AIController {

    // This will automatically look for the environment variable named GEMINI_API_KEY 
    // that you set in your Render dashboard settings.
    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody String jsonBody) {
        try {
            // Check if API Key was loaded correctly
            if (geminiApiKey == null || geminiApiKey.isEmpty()) {
                return ResponseEntity.status(500).body("{\"error\": \"API Key is missing from Environment Variables.\"}");
            }

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the request entity using the JSON body from the frontend
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            // UPDATED URL: Using v1beta/models/gemini-1.5-flash
            // This is the most compatible version for new keys and specific regions.
            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-latest:generateContent?key=" + geminiApiKey;

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException e) {
            // This catches Google API specific errors (403, 404, 400)
            return ResponseEntity.status(e.getStatusCode())
                    .body("{\"error\": \"Google API Error: " + e.getResponseBodyAsString() + "\"}");
        } catch (Exception e) {
            // This catches general Java/Backend errors
            return ResponseEntity.status(500)
                    .body("{\"error\": \"Server Error: " + e.getMessage() + "\"}");
        }
    }
}