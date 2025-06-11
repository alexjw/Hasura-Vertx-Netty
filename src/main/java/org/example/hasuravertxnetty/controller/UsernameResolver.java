package org.example.hasuravertxnetty.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.example.hasuravertxnetty.Models.UsernameOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Example For a Custom Resolver, pending
@Component
public class UsernameResolver implements GraphQLQueryResolver {
    private static final Logger logger = LoggerFactory.getLogger(UsernameResolver.class);
    private final RestClient restClient;
    private final String hasuraEndpoint;
    private final String hasuraAdminSecret;
    private final ObjectMapper objectMapper;

    public UsernameResolver(RestClient.Builder restClientBuilder,
                            @Value("${hasura.endpoint}") String hasuraEndpoint,
                            @Value("${hasura.admin-secret}") String hasuraAdminSecret,
                            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.hasuraEndpoint = hasuraEndpoint;
        this.hasuraAdminSecret = hasuraAdminSecret;
        this.objectMapper = objectMapper;
    }

    public UsernameOutput appendHeroToUsername(int playerId) throws Exception {
        logger.info("Processing appendHeroToUsername for playerId: {}", playerId);

        if (playerId <= 0) {
            logger.error("Invalid playerId: {}", playerId);
            throw new IllegalArgumentException("Invalid playerId");
        }

        // Query Hasura for username
        String query = "{\"query\": \"query { players(where: {id: {_eq: " + playerId + "}}) { id username } }\"}";
        String response = restClient.post()
                .uri(hasuraEndpoint)
                .header("x-hasura-admin-secret", hasuraAdminSecret)
                .body(query)
                .retrieve()
                .body(String.class);

        // Parse response with Jackson
        JsonNode node = objectMapper.readTree(response);
        JsonNode player = node.path("data").path("players").get(0);
        if (player == null) {
            logger.error("Player not found for playerId: {}", playerId);
            throw new Exception("Player not found");
        }

        int id = player.path("id").asInt();
        String username = player.path("username").asText();
        String modifiedUsername = username + "_Hero";

        logger.info("Modified username: {}", modifiedUsername);
        return new UsernameOutput(id, modifiedUsername);
    }
}
