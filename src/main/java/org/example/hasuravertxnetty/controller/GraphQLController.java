package org.example.hasuravertxnetty.controller;

import org.example.hasuravertxnetty.services.HasuraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GraphQLController {

    private final HasuraService hasuraService;

    public GraphQLController(HasuraService hasuraService) {
        this.hasuraService = hasuraService;
    }

    @PostMapping("/graphql")
    public ResponseEntity<String> fetchPlayersWithQuery(@RequestBody String query) {
        try {
            String players = hasuraService.freeQuery(query);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching players: " + e.getMessage());
        }
    }

}
