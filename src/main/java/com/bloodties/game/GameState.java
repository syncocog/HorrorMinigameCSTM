package com.bloodties.game;

public enum GameState {
    
    LOBBY("Lobby", "Waiting for players..."),
    STARTING("Starting", "Game is starting..."),
    SPAWN("Spawn", "Players are spawning..."),
    ACTION("Action", "Game in progress"),
    VOTING("Voting", "Voting phase"),
    CONSEQUENCE("Consequence", "Processing votes..."),
    ENDING("Ending", "Game ending..."),
    FINISHED("Finished", "Game finished");
    
    private final String displayName;
    private final String description;
    
    GameState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == ACTION || this == VOTING || this == CONSEQUENCE;
    }
    
    public boolean isWaiting() {
        return this == LOBBY || this == STARTING || this == SPAWN;
    }
    
    public boolean isFinished() {
        return this == ENDING || this == FINISHED;
    }
}