package org.example.hasuravertxnetty.controller;

import org.example.hasuravertxnetty.services.HasuraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/example")
public class ExampleController {

    private final HasuraService hasuraService;

    public ExampleController(HasuraService hasuraService) {
        this.hasuraService = hasuraService;
    }

    @GetMapping
    public ResponseEntity<String> getAllPlayers() {
        try {
            String players = hasuraService.fetchPlayers();
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching players: " + e.getMessage());
        }
    }
}
