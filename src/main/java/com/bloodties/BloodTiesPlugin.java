package com.bloodties;

import com.bloodties.commands.BloodTiesCommand;
import com.bloodties.game.GameManager;
import com.bloodties.listeners.GameListener;
import com.bloodties.listeners.LobbyListener;
import com.bloodties.managers.ConfigManager;
import com.bloodties.managers.DataManager;
import com.bloodties.managers.SoundManager;
import com.bloodties.utils.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class BloodTiesPlugin extends JavaPlugin {
    
    private static BloodTiesPlugin instance;
    private GameManager gameManager;
    private ConfigManager configManager;
    private DataManager dataManager;
    private SoundManager soundManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        Logger.info("Starting Blood Ties plugin...");
        
        // Initialize managers
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        soundManager = new SoundManager(this);
        gameManager = new GameManager(this);
        
        // Register commands
        getCommand("bt").setExecutor(new BloodTiesCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        
        Logger.info("Blood Ties plugin has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        Logger.info("Shutting down Blood Ties plugin...");
        
        if (gameManager != null) {
            gameManager.shutdown();
        }
        
        if (dataManager != null) {
            dataManager.saveAllData();
        }
        
        Logger.info("Blood Ties plugin has been disabled.");
    }
    
    public static BloodTiesPlugin getInstance() {
        return instance;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public SoundManager getSoundManager() {
        return soundManager;
    }
}