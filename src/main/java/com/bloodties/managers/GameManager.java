package com.bloodties.managers;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.game.Game;
import com.bloodties.game.GameMode;
import com.bloodties.game.GameState;
import com.bloodties.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private final BloodTiesPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    
    private final Map<GameMode, Game> activeGames;
    private final Map<UUID, GameMode> playerQueues;
    private final Map<UUID, Long> heartbeatTimers;
    
    public GameManager(BloodTiesPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        
        this.activeGames = new ConcurrentHashMap<>();
        this.playerQueues = new ConcurrentHashMap<>();
        this.heartbeatTimers = new ConcurrentHashMap<>();
    }
    
    public void joinGame(Player player, GameMode mode) {
        UUID playerId = player.getUniqueId();
        
        // Check if player is already in a game
        if (isPlayerInGame(playerId)) {
            player.sendMessage("§c§l❌ §7You are already in a game!");
            return;
        }
        
        // Check if player is already in queue
        if (playerQueues.containsKey(playerId)) {
            player.sendMessage("§c§l❌ §7You are already in queue for " + playerQueues.get(playerId).getDisplayName() + "!");
            return;
        }
        
        // Get or create game for this mode
        Game game = activeGames.get(mode);
        if (game == null) {
            game = new Game(mode);
            activeGames.put(mode, game);
            Logger.info("Created new Blood Ties game for " + mode.getDisplayName() + " mode");
        }
        
        // Check if game is full
        if (game.getPlayers().size() >= configManager.getMaxPlayers()) {
            player.sendMessage("§c§l❌ §7Game is full! Try another mode or wait for a spot.");
            return;
        }
        
        // Add player to game
        game.addPlayer(playerId, player.getName());
        playerQueues.put(playerId, mode);
        
        player.sendMessage("§a§l✅ §7You joined the §e" + mode.getDisplayName() + " §7queue!");
        player.sendMessage("§7Players: §e" + game.getPlayers().size() + "/" + configManager.getMaxPlayers());
        
        // Start heartbeat timer
        startHeartbeatTimer(playerId);
        
        Logger.info(player.getName() + " joined " + mode.getDisplayName() + " queue");
    }
    
    public void leaveGame(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check if player is in queue
        GameMode queuedMode = playerQueues.get(playerId);
        if (queuedMode != null) {
            Game game = activeGames.get(queuedMode);
            if (game != null) {
                game.removePlayer(playerId);
                
                // Remove game if empty
                if (game.getPlayers().isEmpty()) {
                    activeGames.remove(queuedMode);
                    Logger.info("Removed empty " + queuedMode.getDisplayName() + " game");
                }
            }
            
            playerQueues.remove(playerId);
            stopHeartbeatTimer(playerId);
            
            player.sendMessage("§a§l✅ §7You left the §e" + queuedMode.getDisplayName() + " §7queue!");
            Logger.info(player.getName() + " left " + queuedMode.getDisplayName() + " queue");
            return;
        }
        
        // Check if player is in active game
        Game activeGame = getPlayerGame(playerId);
        if (activeGame != null) {
            activeGame.removePlayer(playerId);
            player.sendMessage("§a§l✅ §7You left the game!");
            Logger.info(player.getName() + " left active game");
            return;
        }
        
        player.sendMessage("§c§l❌ §7You are not in any game or queue!");
    }
    
    public void forceStartGame(Player admin, GameMode mode) {
        if (!admin.hasPermission("bloodties.admin")) {
            admin.sendMessage("§c§l❌ §7You don't have permission to force start games!");
            return;
        }
        
        Game game = activeGames.get(mode);
        if (game == null) {
            admin.sendMessage("§c§l❌ §7No active game found for " + mode.getDisplayName() + " mode!");
            return;
        }
        
        if (game.getState() != GameState.LOBBY) {
            admin.sendMessage("§c§l❌ §7Game is already in progress!");
            return;
        }
        
        if (game.getPlayers().size() < configManager.getMinPlayers()) {
            admin.sendMessage("§c§l❌ §7Not enough players! Need at least " + configManager.getMinPlayers());
            return;
        }
        
        game.start();
        admin.sendMessage("§a§l✅ §7Force started " + mode.getDisplayName() + " game!");
        Logger.info(admin.getName() + " force started " + mode.getDisplayName() + " game");
    }
    
    public void stopAllGames(Player admin) {
        if (!admin.hasPermission("bloodties.admin")) {
            admin.sendMessage("§c§l❌ §7You don't have permission to stop games!");
            return;
        }
        
        int stoppedGames = 0;
        for (Map.Entry<GameMode, Game> entry : activeGames.entrySet()) {
            Game game = entry.getValue();
            if (game.getState() != GameState.FINISHED) {
                game.endGame();
                stoppedGames++;
            }
        }
        
        activeGames.clear();
        playerQueues.clear();
        
        // Stop all heartbeat timers
        for (UUID playerId : heartbeatTimers.keySet()) {
            stopHeartbeatTimer(playerId);
        }
        
        admin.sendMessage("§a§l✅ §7Stopped " + stoppedGames + " active games!");
        Logger.info(admin.getName() + " stopped " + stoppedGames + " games");
    }
    
    public void kickPlayer(Player admin, String targetName) {
        if (!admin.hasPermission("bloodties.admin")) {
            admin.sendMessage("§c§l❌ §7You don't have permission to kick players!");
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            admin.sendMessage("§c§l❌ §7Player " + targetName + " not found!");
            return;
        }
        
        UUID targetId = target.getUniqueId();
        
        // Check if player is in queue
        GameMode queuedMode = playerQueues.get(targetId);
        if (queuedMode != null) {
            Game game = activeGames.get(queuedMode);
            if (game != null) {
                game.removePlayer(targetId);
                
                // Remove game if empty
                if (game.getPlayers().isEmpty()) {
                    activeGames.remove(queuedMode);
                }
            }
            
            playerQueues.remove(targetId);
            stopHeartbeatTimer(targetId);
            
            admin.sendMessage("§a§l✅ §7Kicked " + target.getName() + " from " + queuedMode.getDisplayName() + " queue!");
            target.sendMessage("§c§l❌ §7You were kicked from the game by an admin!");
            Logger.info(admin.getName() + " kicked " + target.getName() + " from queue");
            return;
        }
        
        // Check if player is in active game
        Game activeGame = getPlayerGame(targetId);
        if (activeGame != null) {
            activeGame.removePlayer(targetId);
            
            admin.sendMessage("§a§l✅ §7Kicked " + target.getName() + " from active game!");
            target.sendMessage("§c§l❌ §7You were kicked from the game by an admin!");
            Logger.info(admin.getName() + " kicked " + target.getName() + " from active game");
            return;
        }
        
        admin.sendMessage("§c§l❌ §7Player " + target.getName() + " is not in any game or queue!");
    }
    
    public void showServerStats(Player admin) {
        if (!admin.hasPermission("bloodties.admin")) {
            admin.sendMessage("§c§l❌ §7You don't have permission to view server stats!");
            return;
        }
        
        admin.sendMessage("§c§l📊 §7Blood Ties Server Statistics");
        admin.sendMessage("§7Active Games: §e" + getActiveGameCount());
        admin.sendMessage("§7Players in Queue: §e" + playerQueues.size());
        admin.sendMessage("§7Total Players: §e" + getTotalPlayerCount());
        
        // Show stats for each mode
        for (GameMode mode : GameMode.values()) {
            Game game = activeGames.get(mode);
            if (game != null) {
                admin.sendMessage("§7" + mode.getDisplayName() + ": §e" + game.getPlayers().size() + " players §7(State: " + game.getState().getDisplayName() + ")");
            } else {
                admin.sendMessage("§7" + mode.getDisplayName() + ": §cNo active game");
            }
        }
        
        // Show top players
        admin.sendMessage("§7Top Players by Karma:");
        List<Map.Entry<String, Integer>> topPlayers = dataManager.getTopPlayersByKarma(5);
        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<String, Integer> entry = topPlayers.get(i);
            admin.sendMessage("§7" + (i + 1) + ". §e" + entry.getKey() + " §7- §a" + entry.getValue() + " karma");
        }
    }
    
    public void broadcastToAll(String message) {
        for (Map.Entry<GameMode, Game> entry : activeGames.entrySet()) {
            Game game = entry.getValue();
            if (game.getState() != GameState.FINISHED) {
                game.broadcastMessage("§c§l📢 §7" + message);
            }
        }
        
        // Also send to players in queue
        for (UUID playerId : playerQueues.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§c§l📢 §7" + message);
            }
        }
    }
    
    public void reloadConfig(Player admin) {
        if (!admin.hasPermission("bloodties.admin")) {
            admin.sendMessage("§c§l❌ §7You don't have permission to reload config!");
            return;
        }
        
        configManager.loadConfig();
        admin.sendMessage("§a§l✅ §7Configuration reloaded!");
        Logger.info(admin.getName() + " reloaded configuration");
    }
    
    public void debugGame(Player admin, GameMode mode) {
        if (!admin.hasPermission("bloodties.admin")) {
            admin.sendMessage("§c§l❌ §7You don't have permission to debug games!");
            return;
        }
        
        Game game = activeGames.get(mode);
        if (game == null) {
            admin.sendMessage("§c§l❌ §7No active game found for " + mode.getDisplayName() + " mode!");
            return;
        }
        
        admin.sendMessage("§c§l🐛 §7Debug Info for " + mode.getDisplayName() + " Game:");
        admin.sendMessage("§7State: §e" + game.getState().getDisplayName());
        admin.sendMessage("§7Players: §e" + game.getPlayers().size());
        admin.sendMessage("§7Alive: §e" + game.getAlivePlayers().size());
        admin.sendMessage("§7Dead: §e" + game.getDeadPlayers().size());
        admin.sendMessage("§7Round: §e" + game.getRoundNumber());
        admin.sendMessage("§7Time Left: §e" + game.getTimeLeft() + "s");
        
        // Show player details
        admin.sendMessage("§7Players:");
        for (Map.Entry<UUID, com.bloodties.game.GamePlayer> entry : game.getPlayers().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            com.bloodties.game.GamePlayer gamePlayer = entry.getValue();
            if (player != null) {
                admin.sendMessage("§7- §e" + player.getName() + " §7(" + gamePlayer.getRole().getDisplayName() + ") §7- Alive: " + gamePlayer.isAlive());
            }
        }
    }
    
    private void startHeartbeatTimer(UUID playerId) {
        if (heartbeatTimers.containsKey(playerId)) {
            stopHeartbeatTimer(playerId);
        }
        
        heartbeatTimers.put(playerId, System.currentTimeMillis());
    }
    
    private void stopHeartbeatTimer(UUID playerId) {
        heartbeatTimers.remove(playerId);
    }
    
    public boolean isPlayerInGame(UUID playerId) {
        return getPlayerGame(playerId) != null;
    }
    
    public Game getPlayerGame(UUID playerId) {
        for (Game game : activeGames.values()) {
            if (game.getPlayers().containsKey(playerId)) {
                return game;
            }
        }
        return null;
    }
    
    public Game getGame(GameMode mode) {
        return activeGames.get(mode);
    }
    
    public int getActiveGameCount() {
        return (int) activeGames.values().stream()
            .filter(game -> game.getState() != GameState.FINISHED)
            .count();
    }
    
    public int getTotalPlayerCount() {
        int count = 0;
        for (Game game : activeGames.values()) {
            if (game.getState() != GameState.FINISHED) {
                count += game.getPlayers().size();
            }
        }
        return count + playerQueues.size();
    }
    
    public void cleanup() {
        // Stop all games
        for (Game game : activeGames.values()) {
            if (game.getState() != GameState.FINISHED) {
                game.endGame();
            }
        }
        
        activeGames.clear();
        playerQueues.clear();
        heartbeatTimers.clear();
        
        Logger.info("GameManager cleanup completed");
    }
}