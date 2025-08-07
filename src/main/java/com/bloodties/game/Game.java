package com.bloodties.game;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.gui.VotingGUI;
import com.bloodties.managers.SoundManager;
import com.bloodties.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Game {
    
    private final BloodTiesPlugin plugin;
    private final String gameId;
    private final GameMode gameMode;
    private GameState state;
    private final Map<UUID, GamePlayer> players;
    private final Map<UUID, GamePlayer> alivePlayers;
    private final Map<UUID, GamePlayer> deadPlayers;
    private final Map<UUID, String> votes;
    private final Map<UUID, String> voteOffers;
    private final Map<UUID, Set<UUID>> whispers;
    private int round;
    private int timeRemaining;
    private BukkitTask gameTask;
    private BukkitTask ambientTask;
    private final VotingGUI votingGUI;
    private GamePlayer monster;
    private boolean monsterWon;
    private final List<Location> spawnPoints;
    private Location lobbyLocation;
    
    public Game(BloodTiesPlugin plugin, GameMode gameMode) {
        this.plugin = plugin;
        this.gameId = UUID.randomUUID().toString().substring(0, 8);
        this.gameMode = gameMode;
        this.state = GameState.LOBBY;
        this.players = new ConcurrentHashMap<>();
        this.alivePlayers = new ConcurrentHashMap<>();
        this.deadPlayers = new ConcurrentHashMap<>();
        this.votes = new ConcurrentHashMap<>();
        this.voteOffers = new ConcurrentHashMap<>();
        this.whispers = new ConcurrentHashMap<>();
        this.round = 0;
        this.timeRemaining = 0;
        this.votingGUI = new VotingGUI(this);
        this.monster = null;
        this.monsterWon = false;
        this.spawnPoints = new ArrayList<>();
        this.lobbyLocation = null;
    }
    
    // Game Management
    public void start() {
        if (players.size() < gameMode.getMinPlayers()) {
            broadcastMessage(ChatColor.RED + "Not enough players to start! Need at least " + gameMode.getMinPlayers());
            return;
        }
        
        Logger.info("Starting Blood Ties game " + gameId + " with " + players.size() + " players");
        
        // Assign roles
        assignRoles();
        
        // Set state and start game
        setState(GameState.STARTING);
        startGameLoop();
    }
    
    private void assignRoles() {
        List<GamePlayer> playerList = new ArrayList<>(players.values());
        Collections.shuffle(playerList);
        
        // Assign Monster (always 1)
        monster = playerList.get(0);
        monster.setRole(Role.MONSTER);
        
        // Assign other roles randomly
        for (int i = 1; i < playerList.size(); i++) {
            GamePlayer player = playerList.get(i);
            player.setRole(Role.getRandomSurvivorRole());
        }
        
        // Notify players of their roles
        for (GamePlayer gamePlayer : players.values()) {
            Player player = Bukkit.getPlayer(gamePlayer.getPlayerId());
            if (player != null) {
                sendRoleMessage(player, gamePlayer);
            }
        }
    }
    
    private void sendRoleMessage(Player player, GamePlayer gamePlayer) {
        Role role = gamePlayer.getRole();
        
        player.sendMessage(ChatColor.DARK_RED + "╔══════════════════════════════════════╗");
        player.sendMessage(ChatColor.DARK_RED + "║           " + ChatColor.WHITE + "BLOOD TIES" + ChatColor.DARK_RED + "           ║");
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        player.sendMessage(ChatColor.DARK_RED + "║  " + role.getColoredName() + ChatColor.DARK_RED + "  ║");
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        player.sendMessage(ChatColor.WHITE + role.getDescription());
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        
        for (String ability : role.getAbilities()) {
            player.sendMessage(ChatColor.GRAY + ability);
        }
        
        player.sendMessage(ChatColor.DARK_RED + "╚══════════════════════════════════════╝");
        
        // Play role assignment sound
        plugin.getSoundManager().playGameStart(player);
    }
    
    private void startGameLoop() {
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                switch (state) {
                    case STARTING:
                        handleStarting();
                        break;
                    case SPAWN:
                        handleSpawn();
                        break;
                    case ACTION:
                        handleAction();
                        break;
                    case VOTING:
                        handleVoting();
                        break;
                    case CONSEQUENCE:
                        handleConsequence();
                        break;
                    case ENDING:
                        handleEnding();
                        break;
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
        
        // Start ambient sounds
        startAmbientSounds();
    }
    
    private void handleStarting() {
        if (timeRemaining <= 0) {
            setState(GameState.SPAWN);
            timeRemaining = 30; // 30 seconds spawn time
            teleportPlayersToArena();
        } else {
            broadcastMessage(ChatColor.YELLOW + "Game starting in " + timeRemaining + " seconds...");
            timeRemaining--;
        }
    }
    
    private void handleSpawn() {
        if (timeRemaining <= 0) {
            setState(GameState.ACTION);
            timeRemaining = plugin.getConfigManager().getRoundDuration();
            round = 1;
            broadcastMessage(ChatColor.GREEN + "The game has begun! Find the Monster before it's too late!");
        } else {
            if (timeRemaining % 10 == 0 || timeRemaining <= 5) {
                broadcastMessage(ChatColor.YELLOW + "Exploration time: " + timeRemaining + " seconds");
            }
            timeRemaining--;
        }
    }
    
    private void handleAction() {
        if (timeRemaining <= 0) {
            setState(GameState.VOTING);
            timeRemaining = plugin.getConfigManager().getVoteDuration();
            startVoting();
        } else {
            if (timeRemaining % 30 == 0 || timeRemaining <= 10) {
                broadcastMessage(ChatColor.YELLOW + "Voting begins in " + timeRemaining + " seconds");
            }
            timeRemaining--;
        }
    }
    
    private void handleVoting() {
        if (timeRemaining <= 0) {
            setState(GameState.CONSEQUENCE);
            timeRemaining = 10; // 10 seconds for consequence
            processVotes();
        } else {
            if (timeRemaining % 10 == 0 || timeRemaining <= 5) {
                broadcastMessage(ChatColor.YELLOW + "Voting ends in " + timeRemaining + " seconds");
            }
            timeRemaining--;
        }
    }
    
    private void handleConsequence() {
        if (timeRemaining <= 0) {
            // Check win conditions
            if (checkWinConditions()) {
                setState(GameState.ENDING);
                timeRemaining = 10;
            } else {
                // Start next round
                setState(GameState.ACTION);
                timeRemaining = plugin.getConfigManager().getRoundDuration();
                round++;
                resetForNewRound();
            }
        } else {
            timeRemaining--;
        }
    }
    
    private void handleEnding() {
        if (timeRemaining <= 0) {
            endGame();
        } else {
            timeRemaining--;
        }
    }
    
    private void startVoting() {
        votes.clear();
        voteOffers.clear();
        
        // Open voting GUI for all players
        for (GamePlayer gamePlayer : alivePlayers.values()) {
            Player player = Bukkit.getPlayer(gamePlayer.getPlayerId());
            if (player != null) {
                votingGUI.open(player);
                plugin.getSoundManager().playVoteStart(player);
            }
        }
        
        broadcastMessage(ChatColor.GOLD + "Voting phase has begun! Vote for who you think is the Monster!");
    }
    
    private void processVotes() {
        // Count votes
        Map<String, Integer> voteCounts = new HashMap<>();
        for (String target : votes.values()) {
            voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
        }
        
        // Find highest voted player(s)
        int maxVotes = voteCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> highestVoted = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() == maxVotes) {
                highestVoted.add(entry.getKey());
            }
        }
        
        // Process sacrifice
        if (!highestVoted.isEmpty() && maxVotes > 0) {
            String sacrificedPlayer = highestVoted.get(0);
            GamePlayer sacrificed = getPlayerByName(sacrificedPlayer);
            
            if (sacrificed != null && sacrificed.isAlive()) {
                sacrificePlayer(sacrificed);
            }
        } else {
            broadcastMessage(ChatColor.YELLOW + "No one was sacrificed this round.");
        }
    }
    
    public Map<UUID, String> getVotes() {
        return votes;
    }
    
    public Map<UUID, GamePlayer> getDeadPlayers() {
        return deadPlayers;
    }
    

    
    private void sacrificePlayer(GamePlayer gamePlayer) {
        gamePlayer.setAlive(false);
        alivePlayers.remove(gamePlayer.getPlayerId());
        deadPlayers.put(gamePlayer.getPlayerId(), gamePlayer);
        
        Player player = Bukkit.getPlayer(gamePlayer.getPlayerId());
        if (player != null) {
            // Play sacrifice animation and sound
            plugin.getSoundManager().playSacrifice(player.getLocation());
            
            // Kill the player
            player.setHealth(0);
            
            // Reveal their role
            broadcastMessage(ChatColor.RED + "☠ " + gamePlayer.getName() + " was sacrificed!");
            broadcastMessage(ChatColor.GRAY + "They were: " + gamePlayer.getRole().getColoredName());
            
            // Award karma
            if (gamePlayer.isMonster()) {
                // Award karma to players who voted correctly
                for (Map.Entry<UUID, String> vote : votes.entrySet()) {
                    if (vote.getValue().equals(gamePlayer.getName())) {
                        GamePlayer voter = players.get(vote.getKey());
                        if (voter != null) {
                            voter.incrementCorrectVotes();
                            plugin.getDataManager().addKarma(voter.getPlayerId(), 
                                plugin.getConfigManager().getKarmaVoteMonster());
                        }
                    }
                }
            } else {
                // Penalize players who voted incorrectly
                for (Map.Entry<UUID, String> vote : votes.entrySet()) {
                    if (vote.getValue().equals(gamePlayer.getName())) {
                        GamePlayer voter = players.get(vote.getKey());
                        if (voter != null) {
                            plugin.getDataManager().addKarma(voter.getPlayerId(), 
                                plugin.getConfigManager().getKarmaVoteInnocent());
                        }
                    }
                }
            }
        }
    }
    
    public boolean checkWinConditions() {
        // Check if Monster won
        if (alivePlayers.size() <= 1) {
            monsterWon = true;
            return true;
        }
        
        // Check if Monster was eliminated
        if (!monster.isAlive()) {
            monsterWon = false;
            return true;
        }
        
        // Check for secret win conditions
        for (GamePlayer player : alivePlayers.values()) {
            if (player.hasWonSecretPath()) {
                // Secret win condition met
                return true;
            }
        }
        
        return false;
    }
    
    private void resetForNewRound() {
        // Reset cooldowns for new round
        for (GamePlayer player : alivePlayers.values()) {
            player.resetCooldowns();
        }
        
        // Clear votes
        votes.clear();
        voteOffers.clear();
        
        broadcastMessage(ChatColor.GREEN + "Round " + round + " has begun!");
    }
    
    private void endGame() {
        setState(GameState.FINISHED);
        
        // Stop tasks
        if (gameTask != null) {
            gameTask.cancel();
        }
        if (ambientTask != null) {
            ambientTask.cancel();
        }
        
        // Announce winner
        if (monsterWon) {
            broadcastMessage(ChatColor.DARK_RED + "☠ The Monster has won! ☠");
            if (monster != null) {
                broadcastMessage(ChatColor.RED + "The Monster was: " + monster.getName());
            }
        } else {
            broadcastMessage(ChatColor.GREEN + "☀ The Survivors have won! ☀");
        }
        
        // Save statistics
        saveGameStats();
        
        // Return players to lobby
        teleportPlayersToLobby();
        
        // Clean up
        plugin.getGameManager().removeGame(this);
    }
    
    private void saveGameStats() {
        for (GamePlayer gamePlayer : players.values()) {
            // Increment games played
            plugin.getDataManager().incrementGamesPlayed(gamePlayer.getPlayerId());
            
            // Save player stats
            gamePlayer.saveStats();
            
            // Award karma for surviving
            if (gamePlayer.isAlive()) {
                plugin.getDataManager().addKarma(gamePlayer.getPlayerId(), 
                    plugin.getConfigManager().getKarmaSurviveRound());
            }
        }
        
        // Award wins
        if (monsterWon && monster != null) {
            plugin.getDataManager().incrementGamesWon(monster.getPlayerId());
            plugin.getDataManager().incrementRoleWins(monster.getPlayerId(), "monster");
        } else {
            for (GamePlayer player : alivePlayers.values()) {
                if (!player.isMonster()) {
                    plugin.getDataManager().incrementGamesWon(player.getPlayerId());
                    plugin.getDataManager().incrementRoleWins(player.getPlayerId(), 
                        player.getRole().name().toLowerCase());
                }
            }
        }
    }
    
    private void startAmbientSounds() {
        ambientTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state.isActive()) {
                    // Play random ambient sounds
                    for (GamePlayer gamePlayer : alivePlayers.values()) {
                        Player player = Bukkit.getPlayer(gamePlayer.getPlayerId());
                        if (player != null) {
                            plugin.getSoundManager().playAmbientHorror(player.getLocation());
                            
                            // Play heartbeat near Monster
                            if (monster != null && monster.isAlive()) {
                                Player monsterPlayer = Bukkit.getPlayer(monster.getPlayerId());
                                if (monsterPlayer != null && player.getLocation().distance(monsterPlayer.getLocation()) < 10) {
                                    plugin.getSoundManager().playHeartbeat(player);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Every 5 seconds
    }
    
    // Player Management
    public void addPlayer(Player player) {
        GamePlayer gamePlayer = new GamePlayer(player);
        players.put(player.getUniqueId(), gamePlayer);
        alivePlayers.put(player.getUniqueId(), gamePlayer);
        
        // Update player name in data
        plugin.getDataManager().updatePlayerName(player.getUniqueId(), player.getName());
        
        broadcastMessage(ChatColor.GREEN + player.getName() + " joined the game!");
        
        // Check if we can start
        if (state == GameState.LOBBY && players.size() >= gameMode.getMinPlayers() && 
            plugin.getConfigManager().isAutoStart()) {
            start();
        }
    }
    
    public void removePlayer(Player player) {
        GamePlayer gamePlayer = players.get(player.getUniqueId());
        if (gamePlayer != null) {
            alivePlayers.remove(player.getUniqueId());
            deadPlayers.remove(player.getUniqueId());
            players.remove(player.getUniqueId());
            
            broadcastMessage(ChatColor.RED + player.getName() + " left the game!");
            
            // Check if game should end
            if (alivePlayers.size() < 2) {
                endGame();
            }
        }
    }
    
    public GamePlayer getPlayer(UUID playerId) {
        return players.get(playerId);
    }
    
    public GamePlayer getPlayerByName(String name) {
        for (GamePlayer player : players.values()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
    
    // Voting System
    public void castVote(UUID voterId, String targetName) {
        GamePlayer voter = players.get(voterId);
        if (voter == null || !voter.isAlive()) return;
        
        GamePlayer target = getPlayerByName(targetName);
        if (target == null || !target.isAlive()) return;
        
        votes.put(voterId, targetName);
        voter.incrementVotes();
        
        Player player = Bukkit.getPlayer(voterId);
        if (player != null) {
            plugin.getSoundManager().playVoteCast(player);
            player.sendMessage(ChatColor.GREEN + "You voted for " + targetName);
        }
    }
    
    public void offerVote(UUID offererId, String targetName, String info) {
        voteOffers.put(offererId, targetName + ":" + info);
    }
    
    public void acceptVoteOffer(UUID offererId, UUID accepterId) {
        String offer = voteOffers.get(offererId);
        if (offer != null) {
            String[] parts = offer.split(":", 2);
            if (parts.length == 2) {
                castVote(accepterId, parts[0]);
                // Transfer the vote info
                Player offerer = Bukkit.getPlayer(offererId);
                Player accepter = Bukkit.getPlayer(accepterId);
                if (offerer != null && accepter != null) {
                    accepter.sendMessage(ChatColor.YELLOW + "Vote trade accepted! Info: " + parts[1]);
                }
            }
        }
    }
    
    // Whisper System
    public void sendWhisper(UUID senderId, UUID receiverId, String message) {
        Player sender = Bukkit.getPlayer(senderId);
        Player receiver = Bukkit.getPlayer(receiverId);
        
        if (sender != null && receiver != null) {
            sender.sendMessage(ChatColor.GRAY + "→ " + receiver.getName() + ": " + message);
            receiver.sendMessage(ChatColor.GRAY + "← " + sender.getName() + ": " + message);
            
            // Add to whisper history
            whispers.computeIfAbsent(senderId, k -> new HashSet<>()).add(receiverId);
            whispers.computeIfAbsent(receiverId, k -> new HashSet<>()).add(senderId);
            
            // Play whisper sound
            plugin.getSoundManager().playWhisper(sender);
            plugin.getSoundManager().playWhisper(receiver);
        }
    }
    
    // Utility Methods
    public void broadcastMessage(String message) {
        for (GamePlayer gamePlayer : players.values()) {
            Player player = Bukkit.getPlayer(gamePlayer.getPlayerId());
            if (player != null) {
                player.sendMessage(ChatColor.DARK_RED + "[BloodTies] " + ChatColor.RESET + message);
            }
        }
    }
    
    public void setState(GameState newState) {
        this.state = newState;
        Logger.debug("Game " + gameId + " state changed to: " + newState.getDisplayName());
    }
    
    private void teleportPlayersToArena() {
        // Implementation for teleporting players to arena spawn points
        // This would use the spawnPoints list
    }
    
    private void teleportPlayersToLobby() {
        // Implementation for teleporting players back to lobby
        // This would use the lobbyLocation
    }
    
    // Getters
    public String getGameId() {
        return gameId;
    }
    
    public GameMode getGameMode() {
        return gameMode;
    }
    
    public GameState getState() {
        return state;
    }
    
    public Map<UUID, GamePlayer> getPlayers() {
        return players;
    }
    
    public Map<UUID, GamePlayer> getAlivePlayers() {
        return alivePlayers;
    }
    
    public int getRound() {
        return round;
    }
    
    public int getTimeRemaining() {
        return timeRemaining;
    }
    
    public GamePlayer getMonster() {
        return monster;
    }
    
    public boolean isMonsterWon() {
        return monsterWon;
    }
    
    public VotingGUI getVotingGUI() {
        return votingGUI;
    }
}