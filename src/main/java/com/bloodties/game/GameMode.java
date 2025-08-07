package com.bloodties.game;

import org.bukkit.ChatColor;

public enum GameMode {
    
    SOLO("Solo", "No trusted allies. Full paranoia.", 6, 20, 1),
    DUO("Duo", "You spawn with one known partner. But are they who they say they are?", 6, 20, 2),
    SQUAD("Squad", "Form blood pacts. Betrayal hurts more here.", 8, 20, 4);
    
    private final String displayName;
    private final String description;
    private final int minPlayers;
    private final int maxPlayers;
    private final int teamSize;
    
    GameMode(String displayName, String description, int minPlayers, int maxPlayers, int teamSize) {
        this.displayName = displayName;
        this.description = description;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.teamSize = teamSize;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getMinPlayers() {
        return minPlayers;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public int getTeamSize() {
        return teamSize;
    }
    
    public String getColoredName() {
        switch (this) {
            case SOLO:
                return ChatColor.RED + "⚔ " + displayName;
            case DUO:
                return ChatColor.YELLOW + "👥 " + displayName;
            case SQUAD:
                return ChatColor.LIGHT_PURPLE + "⚔⚔ " + displayName;
            default:
                return displayName;
        }
    }
    
    public boolean allowsTeams() {
        return this != SOLO;
    }
    
    public boolean isSolo() {
        return this == SOLO;
    }
    
    public boolean isDuo() {
        return this == DUO;
    }
    
    public boolean isSquad() {
        return this == SQUAD;
    }
}