package com.bloodties.managers;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.game.PlayerData;
import com.bloodties.utils.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    
    private final BloodTiesPlugin plugin;
    private final Map<UUID, PlayerData> playerDataCache;
    
    public DataManager(BloodTiesPlugin plugin) {
        this.plugin = plugin;
        this.playerDataCache = new HashMap<>();
        loadAllPlayerData();
    }
    
    public PlayerData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, this::loadPlayerData);
    }
    
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    private PlayerData loadPlayerData(UUID playerId) {
        FileConfiguration dataConfig = plugin.getConfigManager().getDataConfig();
        String path = "players." + playerId.toString();
        
        if (!dataConfig.contains(path)) {
            return new PlayerData(playerId);
        }
        
        PlayerData data = new PlayerData(playerId);
        data.setName(dataConfig.getString(path + ".name", "Unknown"));
        data.setKarma(dataConfig.getInt(path + ".karma", 0));
        data.setGamesPlayed(dataConfig.getInt(path + ".games_played", 0));
        data.setGamesWon(dataConfig.getInt(path + ".games_won", 0));
        data.setTotalKills(dataConfig.getInt(path + ".total_kills", 0));
        data.setTotalVotes(dataConfig.getInt(path + ".total_votes", 0));
        data.setCorrectVotes(dataConfig.getInt(path + ".correct_votes", 0));
        
        // Load role wins
        ConfigurationSection roleWinsSection = dataConfig.getConfigurationSection(path + ".role_wins");
        if (roleWinsSection != null) {
            data.setMonsterWins(roleWinsSection.getInt("monster", 0));
            data.setHealerWins(roleWinsSection.getInt("healer", 0));
            data.setWizardWins(roleWinsSection.getInt("wizard", 0));
            data.setSilentOneWins(roleWinsSection.getInt("silent_one", 0));
            data.setSurvivorWins(roleWinsSection.getInt("survivor", 0));
        }
        
        // Load cosmetics
        ConfigurationSection cosmeticsSection = dataConfig.getConfigurationSection(path + ".cosmetics");
        if (cosmeticsSection != null) {
            data.setParticleTrail(cosmeticsSection.getString("particle_trail", "none"));
            data.setSacrificeAnimation(cosmeticsSection.getString("sacrifice_animation", "default"));
            data.setRoleSkin(cosmeticsSection.getString("role_skin", "default"));
        }
        
        return data;
    }
    
    public void savePlayerData(PlayerData data) {
        FileConfiguration dataConfig = plugin.getConfigManager().getDataConfig();
        String path = "players." + data.getPlayerId().toString();
        
        dataConfig.set(path + ".name", data.getName());
        dataConfig.set(path + ".karma", data.getKarma());
        dataConfig.set(path + ".games_played", data.getGamesPlayed());
        dataConfig.set(path + ".games_won", data.getGamesWon());
        dataConfig.set(path + ".total_kills", data.getTotalKills());
        dataConfig.set(path + ".total_votes", data.getTotalVotes());
        dataConfig.set(path + ".correct_votes", data.getCorrectVotes());
        
        // Save role wins
        dataConfig.set(path + ".role_wins.monster", data.getMonsterWins());
        dataConfig.set(path + ".role_wins.healer", data.getHealerWins());
        dataConfig.set(path + ".role_wins.wizard", data.getWizardWins());
        dataConfig.set(path + ".role_wins.silent_one", data.getSilentOneWins());
        dataConfig.set(path + ".role_wins.survivor", data.getSurvivorWins());
        
        // Save cosmetics
        dataConfig.set(path + ".cosmetics.particle_trail", data.getParticleTrail());
        dataConfig.set(path + ".cosmetics.sacrifice_animation", data.getSacrificeAnimation());
        dataConfig.set(path + ".cosmetics.role_skin", data.getRoleSkin());
        
        plugin.getConfigManager().saveDataConfig();
    }
    
    private void loadAllPlayerData() {
        FileConfiguration dataConfig = plugin.getConfigManager().getDataConfig();
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        
        if (playersSection != null) {
            for (String playerIdString : playersSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(playerIdString);
                    playerDataCache.put(playerId, loadPlayerData(playerId));
                } catch (IllegalArgumentException e) {
                    Logger.warning("Invalid player UUID in data.yml: " + playerIdString);
                }
            }
        }
    }
    
    public void saveAllData() {
        for (PlayerData data : playerDataCache.values()) {
            savePlayerData(data);
        }
        Logger.info("Saved data for " + playerDataCache.size() + " players");
    }
    
    public void updatePlayerName(UUID playerId, String name) {
        PlayerData data = getPlayerData(playerId);
        data.setName(name);
        savePlayerData(data);
    }
    
    public void addKarma(UUID playerId, int amount) {
        PlayerData data = getPlayerData(playerId);
        data.setKarma(data.getKarma() + amount);
        savePlayerData(data);
    }
    
    public void incrementGamesPlayed(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        data.setGamesPlayed(data.getGamesPlayed() + 1);
        savePlayerData(data);
    }
    
    public void incrementGamesWon(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        data.setGamesWon(data.getGamesWon() + 1);
        savePlayerData(data);
    }
    
    public void incrementRoleWins(UUID playerId, String role) {
        PlayerData data = getPlayerData(playerId);
        switch (role.toLowerCase()) {
            case "monster":
                data.setMonsterWins(data.getMonsterWins() + 1);
                break;
            case "healer":
                data.setHealerWins(data.getHealerWins() + 1);
                break;
            case "wizard":
                data.setWizardWins(data.getWizardWins() + 1);
                break;
            case "silent_one":
                data.setSilentOneWins(data.getSilentOneWins() + 1);
                break;
            case "survivor":
                data.setSurvivorWins(data.getSurvivorWins() + 1);
                break;
        }
        savePlayerData(data);
    }
    
    public void incrementKills(UUID playerId, int kills) {
        PlayerData data = getPlayerData(playerId);
        data.setTotalKills(data.getTotalKills() + kills);
        savePlayerData(data);
    }
    
    public void incrementVotes(UUID playerId, int votes) {
        PlayerData data = getPlayerData(playerId);
        data.setTotalVotes(data.getTotalVotes() + votes);
        savePlayerData(data);
    }
    
    public List<Map.Entry<String, Integer>> getTopPlayersByKarma(int count) {
        List<Map.Entry<String, Integer>> topPlayers = new ArrayList<>();
        
        for (Map.Entry<UUID, PlayerData> entry : playerDataCache.entrySet()) {
            PlayerData data = entry.getValue();
            topPlayers.add(new AbstractMap.SimpleEntry<>(data.getName(), data.getKarma()));
        }
        
        // Sort by karma (descending)
        topPlayers.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        // Return top N players
        return topPlayers.subList(0, Math.min(count, topPlayers.size()));
    }
}