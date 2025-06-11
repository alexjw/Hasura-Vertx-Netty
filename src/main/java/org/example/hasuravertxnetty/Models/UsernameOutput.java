package org.example.hasuravertxnetty.Models;

// For a custom Resolver, pending
public class UsernameOutput {
    private final int playerId;
    private final String modifiedUsername;

    public UsernameOutput(int playerId, String modifiedUsername) {
        this.playerId = playerId;
        this.modifiedUsername = modifiedUsername;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getModifiedUsername() {
        return modifiedUsername;
    }
}