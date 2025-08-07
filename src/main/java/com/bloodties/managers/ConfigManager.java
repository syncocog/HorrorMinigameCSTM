package com.bloodties.managers;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    
    private final BloodTiesPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration dataConfig;
    private File dataFile;
    
    public ConfigManager(BloodTiesPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadDataConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Set default values if they don't exist
        setDefaults();
    }
    
    private void setDefaults() {
        config.addDefault("game.min_players", 6);
        config.addDefault("game.max_players", 20);
        config.addDefault("game.round_duration", 180); // 3 minutes
        config.addDefault("game.vote_duration", 30); // 30 seconds
        config.addDefault("game.lobby_duration", 60); // 1 minute
        
        config.addDefault("roles.monster_kill_cooldown", 300); // 5 minutes
        config.addDefault("roles.healer_heal_cooldown", 600); // 10 minutes
        config.addDefault("roles.wizard_spell_cooldown", 120); // 2 minutes
        
        config.addDefault("karma.vote_monster", 10);
        config.addDefault("karma.vote_innocent", -3);
        config.addDefault("karma.betray_team", -5);
        config.addDefault("karma.survive_round", 1);
        
        config.addDefault("sounds.enabled", true);
        config.addDefault("sounds.volume", 0.5);
        config.addDefault("sounds.pitch", 1.0);
        
        config.addDefault("debug_mode", false);
        config.addDefault("auto_start", true);
        config.addDefault("lobby_world", "world");
        config.addDefault("arena_world", "bloodties_arena");
        
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }
    
    private void loadDataConfig() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void saveDataConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            Logger.error("Could not save data.yml: " + e.getMessage());
        }
    }
    
    public FileConfiguration getDataConfig() {
        return dataConfig;
    }
    
    // Game settings
    public int getMinPlayers() {
        return config.getInt("game.min_players", 6);
    }
    
    public int getMaxPlayers() {
        return config.getInt("game.max_players", 20);
    }
    
    public int getRoundDuration() {
        return config.getInt("game.round_duration", 180);
    }
    
    public int getVoteDuration() {
        return config.getInt("game.vote_duration", 30);
    }
    
    public int getLobbyDuration() {
        return config.getInt("game.lobby_duration", 60);
    }
    
    // Role settings
    public int getMonsterKillCooldown() {
        return config.getInt("roles.monster_kill_cooldown", 300);
    }
    
    public int getHealerHealCooldown() {
        return config.getInt("roles.healer_heal_cooldown", 600);
    }
    
    public int getWizardSpellCooldown() {
        return config.getInt("roles.wizard_spell_cooldown", 120);
    }
    
    // Karma settings
    public int getKarmaVoteMonster() {
        return config.getInt("karma.vote_monster", 10);
    }
    
    public int getKarmaVoteInnocent() {
        return config.getInt("karma.vote_innocent", -3);
    }
    
    public int getKarmaBetrayTeam() {
        return config.getInt("karma.betray_team", -5);
    }
    
    public int getKarmaSurviveRound() {
        return config.getInt("karma.survive_round", 1);
    }
    
    // Sound settings
    public boolean isSoundEnabled() {
        return config.getBoolean("sounds.enabled", true);
    }
    
    public double getSoundVolume() {
        return config.getDouble("sounds.volume", 0.5);
    }
    
    public double getSoundPitch() {
        return config.getDouble("sounds.pitch", 1.0);
    }
    
    // General settings
    public boolean isDebugMode() {
        return config.getBoolean("debug_mode", false);
    }
    
    public boolean isAutoStart() {
        return config.getBoolean("auto_start", true);
    }
    
    public String getLobbyWorld() {
        return config.getString("lobby_world", "world");
    }
    
    public String getArenaWorld() {
        return config.getString("arena_world", "bloodties_arena");
    }
}