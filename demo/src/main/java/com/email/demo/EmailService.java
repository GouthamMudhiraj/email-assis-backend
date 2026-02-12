package com.email.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailService {
    private final WebClient webClient;
    private final String apiKey;

    public EmailService(WebClient.Builder webClientBuilder,
                        @Value("${gemini.api.url}") String baseUrl,
                        @Value("${gemini.api.key}") String geminiApiKey){
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = geminiApiKey;
    }

    public String generateEmailReply(EmailRequest emailRequest) {


        //prompt
        String prompt=escapeJson(buildPrompt(emailRequest));
        //raw json
        String requestBody=String.format("""
                {
                    "contents": [
                      {
                        "parts": [
                          {
                            "text": "%s"
                          }
                        ]
                      }
                    ]
                  }""",prompt);
        //SEND REQUEST
        String response=webClient.post().uri(uriBuilder -> uriBuilder.path("/v1beta/models/gemini-3-flash-preview:generateContent")
                .build())
                .header("x-goog-api-key",apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //extract response
       return extractResponseContent(response);

    }
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }


    private String extractResponseContent(String response) {
        try {
            ObjectMapper objectMapper=new ObjectMapper();

            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an email writing assistant." +
                "\n" +
                "Task:\n" +
                "Write exactly ONE professional email reply.\n" +
                "\n" +
                "Rules (must follow):\n" +
                "- Do NOT provide multiple options or versions\n" +
                "- Do NOT include headings, bullet points, or explanations\n" +
                "- Do NOT say phrases like \"Here are some options\" or \"To give you the best option\"\n" +
                "- Do NOT use markdown\n" +
                "- Output ONLY the final email content\n" +
                "\n" +
                "Tone: friendly and professional\n" +
                "\n" +
                "Include:\n" +
                "- Greeting\n" +
                "- Email body\n" +
                "- Proper closing\n" +
                "\n");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a").append(emailRequest.getTone()).append(" tone ");
        }
        prompt.append("orginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();

    }
}
