package org.example.hasuravertxnetty.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;


// Actions example
@RestController
public class RankController {
    private final RestClient restClient;
    private final String hasuraEndpoint;
    private final String hasuraAdminSecret;

    public RankController(RestClient.Builder restClientBuilder,
                          @Value("${hasura.endpoint}") String hasuraEndpoint,
                          @Value("${hasura.admin-secret}") String hasuraAdminSecret) {
        this.restClient = restClientBuilder.build();
        this.hasuraEndpoint = hasuraEndpoint;
        this.hasuraAdminSecret = hasuraAdminSecret;
    }

    // Action, Hasura will reach this to calculate player rank
    @PostMapping("/api/rank")
    public ResponseEntity<PlayerRankOutput> calculatePlayerRank(
            @RequestHeader(value = "x-hasura-admin-secret", required = false) String adminSecret,
            @RequestBody Map<String, Object> input) {

        //System.out.println("Received payload: {}" + input);
        //System.out.println("Player ID: {}" + input.getPlayerId());

        // Verify admin secret (optional)
        if (adminSecret != null && !adminSecret.equals(hasuraAdminSecret)) {
            return ResponseEntity.status(401).build();
        }

        // Query Hasura for player data
        String query = "{\"query\": \"query { player_battle_summary(where: {player_id: {_eq: " + input.get("playerId") + "}}) { username total_score } }\"}";
        String response = restClient.post()
                .uri(hasuraEndpoint)
                .header("x-hasura-admin-secret", hasuraAdminSecret)
                .body(query)
                .retrieve()
                .body(String.class);

        // Parse response (simplified, use JSON library like Jackson in production)
        String username = response.contains("username") ? extractUsername(response) : "Unknown";
        int totalScore = response.contains("total_score") ? extractTotalScore(response) : 0;

        // Calculate rank
        String rank = calculateRank(totalScore);

        // Return output
        PlayerRankOutput output = new PlayerRankOutput((Integer) ((Map<Object, Object>)input.get("input")).get("playerId"), username, totalScore, rank);
        return ResponseEntity.ok(output);
    }

    private String calculateRank(int totalScore) {
        if (totalScore >= 1000) return "Ace";
        if (totalScore >= 500) return "Veteran";
        return "Rookie";
    }

    private String extractUsername(String response) {
        // Simplified parsing, use Jackson in production
        return response.split("\"username\":\"")[1].split("\"")[0];
    }

    private int extractTotalScore(String response) {
        // Simplified parsing, use Jackson in production
        return Integer.parseInt(response.split("\"total_score\":")[1].split("}")[0]);
    }
}

class PlayerRankOutput {
    private int playerId;
    private String username;
    private int totalScore;
    private String rank;

    public PlayerRankOutput(int playerId, String username, int totalScore, String rank) {
        this.playerId = playerId;
        this.username = username;
        this.totalScore = totalScore;
        this.rank = rank;
    }

    // Getters and setters
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }
}
