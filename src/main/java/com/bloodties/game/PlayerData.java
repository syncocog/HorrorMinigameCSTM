package com.bloodties.game;

import java.util.UUID;

public class PlayerData {
    
    private final UUID playerId;
    private String name;
    private int karma;
    private int gamesPlayed;
    private int gamesWon;
    private int totalKills;
    private int totalVotes;
    private int correctVotes;
    
    // Role-specific wins
    private int monsterWins;
    private int healerWins;
    private int wizardWins;
    private int silentOneWins;
    private int survivorWins;
    
    // Cosmetics
    private String particleTrail;
    private String sacrificeAnimation;
    private String roleSkin;
    
    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.name = "Unknown";
        this.karma = 0;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.totalKills = 0;
        this.totalVotes = 0;
        this.correctVotes = 0;
        this.monsterWins = 0;
        this.healerWins = 0;
        this.wizardWins = 0;
        this.silentOneWins = 0;
        this.survivorWins = 0;
        this.particleTrail = "none";
        this.sacrificeAnimation = "default";
        this.roleSkin = "default";
    }
    
    // Getters and Setters
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getKarma() {
        return karma;
    }
    
    public void setKarma(int karma) {
        this.karma = karma;
    }
    
    public int getGamesPlayed() {
        return gamesPlayed;
    }
    
    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }
    
    public int getGamesWon() {
        return gamesWon;
    }
    
    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }
    
    public int getTotalKills() {
        return totalKills;
    }
    
    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }
    
    public int getTotalVotes() {
        return totalVotes;
    }
    
    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }
    
    public int getCorrectVotes() {
        return correctVotes;
    }
    
    public void setCorrectVotes(int correctVotes) {
        this.correctVotes = correctVotes;
    }
    
    public int getMonsterWins() {
        return monsterWins;
    }
    
    public void setMonsterWins(int monsterWins) {
        this.monsterWins = monsterWins;
    }
    
    public int getHealerWins() {
        return healerWins;
    }
    
    public void setHealerWins(int healerWins) {
        this.healerWins = healerWins;
    }
    
    public int getWizardWins() {
        return wizardWins;
    }
    
    public void setWizardWins(int wizardWins) {
        this.wizardWins = wizardWins;
    }
    
    public int getSilentOneWins() {
        return silentOneWins;
    }
    
    public void setSilentOneWins(int silentOneWins) {
        this.silentOneWins = silentOneWins;
    }
    
    public int getSurvivorWins() {
        return survivorWins;
    }
    
    public void setSurvivorWins(int survivorWins) {
        this.survivorWins = survivorWins;
    }
    
    public String getParticleTrail() {
        return particleTrail;
    }
    
    public void setParticleTrail(String particleTrail) {
        this.particleTrail = particleTrail;
    }
    
    public String getSacrificeAnimation() {
        return sacrificeAnimation;
    }
    
    public void setSacrificeAnimation(String sacrificeAnimation) {
        this.sacrificeAnimation = sacrificeAnimation;
    }
    
    public String getRoleSkin() {
        return roleSkin;
    }
    
    public void setRoleSkin(String roleSkin) {
        this.roleSkin = roleSkin;
    }
    
    // Utility methods
    public double getWinRate() {
        return gamesPlayed > 0 ? (double) gamesWon / gamesPlayed : 0.0;
    }
    
    public double getVoteAccuracy() {
        return totalVotes > 0 ? (double) correctVotes / totalVotes : 0.0;
    }
    
    public int getTotalRoleWins() {
        return monsterWins + healerWins + wizardWins + silentOneWins + survivorWins;
    }
    
    public String getMostPlayedRole() {
        int max = Math.max(Math.max(Math.max(Math.max(monsterWins, healerWins), wizardWins), silentOneWins), survivorWins);
        if (max == monsterWins) return "Monster";
        if (max == healerWins) return "Healer";
        if (max == wizardWins) return "Wizard";
        if (max == silentOneWins) return "Silent One";
        if (max == survivorWins) return "Survivor";
        return "None";
    }
}