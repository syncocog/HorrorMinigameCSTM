package com.bloodties.game;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.utils.Logger;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    
    private final BloodTiesPlugin plugin;
    private final Map<String, Game> activeGames;
    private final Map<UUID, Game> playerGames;
    private final Map<GameMode, Queue<UUID>> gameQueues;
    private final Map<UUID, GameMode> playerQueueMode;
    
    public GameManager(BloodTiesPlugin plugin) {
        this.plugin = plugin;
        this.activeGames = new ConcurrentHashMap<>();
        this.playerGames = new ConcurrentHashMap<>();
        this.gameQueues = new ConcurrentHashMap<>();
        this.playerQueueMode = new ConcurrentHashMap<>();
        
        // Initialize queues for each game mode
        for (GameMode mode : GameMode.values()) {
            gameQueues.put(mode, new LinkedList<>());
        }
    }
    
    // Queue Management
    public boolean addToQueue(Player player, GameMode mode) {
        UUID playerId = player.getUniqueId();
        
        // Check if player is already in a game
        if (playerGames.containsKey(playerId)) {
            player.sendMessage("§cYou are already in a game!");
            return false;
        }
        
        // Check if player is already in queue
        if (playerQueueMode.containsKey(playerId)) {
            player.sendMessage("§cYou are already in queue for " + playerQueueMode.get(playerId).getDisplayName());
            return false;
        }
        
        // Add to queue
        gameQueues.get(mode).offer(playerId);
        playerQueueMode.put(playerId, mode);
        
        player.sendMessage("§aYou have joined the " + mode.getColoredName() + " §aqueue!");
        player.sendMessage("§7Players in queue: " + gameQueues.get(mode).size());
        
        // Check if we can start a game
        checkAndStartGame(mode);
        
        return true;
    }
    
    public boolean removeFromQueue(Player player) {
        UUID playerId = player.getUniqueId();
        
        GameMode mode = playerQueueMode.remove(playerId);
        if (mode != null) {
            gameQueues.get(mode).remove(playerId);
            player.sendMessage("§aYou have left the " + mode.getColoredName() + " §aqueue!");
            return true;
        }
        
        return false;
    }
    
    private void checkAndStartGame(GameMode mode) {
        Queue<UUID> queue = gameQueues.get(mode);
        
        if (queue.size() >= mode.getMinPlayers()) {
            // Create a new game
            Game game = new Game(plugin, mode);
            activeGames.put(game.getGameId(), game);
            
            // Add players to the game
            int playersToAdd = Math.min(queue.size(), mode.getMaxPlayers());
            for (int i = 0; i < playersToAdd; i++) {
                UUID playerId = queue.poll();
                if (playerId != null) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) {
                        game.addPlayer(player);
                        playerGames.put(playerId, game);
                        playerQueueMode.remove(playerId);
                    }
                }
            }
            
            Logger.info("Started new " + mode.getDisplayName() + " game with " + playersToAdd + " players");
        }
    }
    
    // Game Management
    public void removeGame(Game game) {
        activeGames.remove(game.getGameId());
        
        // Remove all players from the game
        for (UUID playerId : game.getPlayers().keySet()) {
            playerGames.remove(playerId);
        }
    }
    
    public Game getPlayerGame(UUID playerId) {
        return playerGames.get(playerId);
    }
    
    public Game getPlayerGame(Player player) {
        return getPlayerGame(player.getUniqueId());
    }
    
    public boolean isPlayerInGame(UUID playerId) {
        return playerGames.containsKey(playerId);
    }
    
    public boolean isPlayerInGame(Player player) {
        return isPlayerInGame(player.getUniqueId());
    }
    
    public boolean isPlayerInQueue(UUID playerId) {
        return playerQueueMode.containsKey(playerId);
    }
    
    public boolean isPlayerInQueue(Player player) {
        return isPlayerInQueue(player.getUniqueId());
    }
    
    public GameMode getPlayerQueueMode(UUID playerId) {
        return playerQueueMode.get(playerId);
    }
    
    public GameMode getPlayerQueueMode(Player player) {
        return getPlayerQueueMode(player.getUniqueId());
    }
    
    // Player Management
    public void handlePlayerJoin(Player player) {
        // Player joined the server, check if they were in a game
        // This could be used for reconnection logic
    }
    
    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Remove from queue if they were queued
        if (isPlayerInQueue(playerId)) {
            removeFromQueue(player);
        }
        
        // Remove from game if they were in one
        Game game = getPlayerGame(playerId);
        if (game != null) {
            game.removePlayer(player);
            playerGames.remove(playerId);
        }
    }
    
    // Admin Commands
    public void forceStartGame(GameMode mode) {
        Queue<UUID> queue = gameQueues.get(mode);
        
        if (queue.size() >= 2) { // Minimum 2 players to force start
            Game game = new Game(plugin, mode);
            activeGames.put(game.getGameId(), game);
            
            // Add all players in queue to the game
            while (!queue.isEmpty()) {
                UUID playerId = queue.poll();
                if (playerId != null) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) {
                        game.addPlayer(player);
                        playerGames.put(playerId, game);
                        playerQueueMode.remove(playerId);
                    }
                }
            }
            
            game.start();
            Logger.info("Force started " + mode.getDisplayName() + " game");
        }
    }
    
    public void stopAllGames() {
        for (Game game : activeGames.values()) {
            game.setState(GameState.ENDING);
        }
        activeGames.clear();
        playerGames.clear();
        
        // Clear all queues
        for (Queue<UUID> queue : gameQueues.values()) {
            queue.clear();
        }
        playerQueueMode.clear();
        
        Logger.info("Stopped all Blood Ties games");
    }
    
    public void kickPlayerFromGame(Player player) {
        Game game = getPlayerGame(player);
        if (game != null) {
            game.removePlayer(player);
            playerGames.remove(player.getUniqueId());
            player.sendMessage("§cYou have been kicked from the game!");
        }
    }
    
    // Statistics
    public int getActiveGamesCount() {
        return activeGames.size();
    }
    
    public int getQueueSize(GameMode mode) {
        return gameQueues.get(mode).size();
    }
    
    public int getTotalPlayersInGames() {
        return playerGames.size();
    }
    
    public List<Game> getActiveGames() {
        return new ArrayList<>(activeGames.values());
    }
    
    public Game getGameById(String gameId) {
        return activeGames.get(gameId);
    }
    
    // Utility Methods
    public void shutdown() {
        stopAllGames();
        Logger.info("GameManager shutdown complete");
    }
    
    public void broadcastToAllGames(String message) {
        for (Game game : activeGames.values()) {
            game.broadcastMessage(message);
        }
    }
    
    public void broadcastToQueue(GameMode mode, String message) {
        Queue<UUID> queue = gameQueues.get(mode);
        for (UUID playerId : queue) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
}