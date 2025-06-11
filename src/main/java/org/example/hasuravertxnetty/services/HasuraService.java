package org.example.hasuravertxnetty.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HasuraService {
    private final RestClient restClient;
    private final String hasuraEndpoint;
    private final String adminSecret;

    public HasuraService(RestClient.Builder restClientBuilder,
                         @Value("${hasura.endpoint}") String hasuraEndpoint,
                         @Value("${hasura.admin-secret}") String adminSecret) {
        this.restClient = restClientBuilder.build();
        this.hasuraEndpoint = hasuraEndpoint;
        this.adminSecret = adminSecret;
    }

    public String fetchPlayers() {
        String query = "{\"query\": \"{ players { id username level } }\"}";
        String query2 = "{\"query\": \"query { battle_participants(where: {battle_id: {_eq: 1}}) { battle_id player { id username } } }\"}";
        String query3 = "{\"query\":\"query { battles(where: {id: {_eq: 1}}) { id battle_participants { score team player { id username level } } } }\"}";
        return restClient.post()
                .uri(hasuraEndpoint)
                .header("x-hasura-admin-secret", adminSecret)
                .body(query3)
                .retrieve()
                .body(String.class);
    }

    public String freeQuery(String query) {
        return restClient.post()
                .uri(hasuraEndpoint)
                .header("x-hasura-admin-secret", adminSecret)
                .body(query)
                .retrieve()
                .body(String.class);
    }


}