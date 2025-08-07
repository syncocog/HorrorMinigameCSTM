package com.bloodties.game;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.gui.VotingGUI;
import com.bloodties.managers.ConfigManager;
import com.bloodties.managers.DataManager;
import com.bloodties.managers.SoundManager;
import com.bloodties.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Game {
    private final BloodTiesPlugin plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final SoundManager soundManager;
    
    private GameState state;
    private GameMode mode;
    private final Map<UUID, GamePlayer> players;
    private final Map<UUID, GamePlayer> alivePlayers;
    private final Map<UUID, GamePlayer> deadPlayers;
    private final Map<UUID, Integer> votes;
    private final Map<UUID, String> voteOffers;
    private final Map<UUID, Long> cooldowns;
    private final Map<UUID, List<UUID>> bloodPacts;
    private final Map<UUID, Integer> soulShards;
    private final Map<UUID, Boolean> silentOneImmunity;
    
    private int roundNumber;
    private int timeLeft;
    private BukkitRunnable gameLoop;
    private BukkitRunnable ambientSounds;
    private BossBar gameBossBar;
    private Scoreboard gameScoreboard;
    private Objective gameObjective;
    
    // Broadcast system
    private final List<String> broadcastMessages;
    private int broadcastIndex;
    private BukkitRunnable broadcastTask;
    
    // Visual effects
    private final Map<UUID, Particle> playerParticles;
    private BukkitRunnable particleTask;
    
    // Auto-start system
    private int autoStartTimer;
    private BukkitRunnable autoStartTask;
    
    public Game(GameMode mode) {
        this.plugin = BloodTiesPlugin.getInstance();
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.soundManager = plugin.getSoundManager();
        
        this.mode = mode;
        this.state = GameState.LOBBY;
        this.players = new ConcurrentHashMap<>();
        this.alivePlayers = new ConcurrentHashMap<>();
        this.deadPlayers = new ConcurrentHashMap<>();
        this.votes = new ConcurrentHashMap<>();
        this.voteOffers = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.bloodPacts = new ConcurrentHashMap<>();
        this.soulShards = new ConcurrentHashMap<>();
        this.silentOneImmunity = new ConcurrentHashMap<>();
        
        this.roundNumber = 0;
        this.timeLeft = 0;
        this.broadcastMessages = new ArrayList<>();
        this.broadcastIndex = 0;
        this.playerParticles = new ConcurrentHashMap<>();
        this.autoStartTimer = configManager.getLobbyDuration();
        
        initializeBroadcastMessages();
        startAutoStartTimer();
        createBossBar();
        createScoreboard();
    }
    
    private void initializeBroadcastMessages() {
        broadcastMessages.addAll(Arrays.asList(
            "§c§l⚔ Blood Ties ⚔ §7A social deduction minigame awaits!",
            "§c§l💀 §7Trust no one. The Monster lurks among you...",
            "§c§l🔮 §7Use your abilities wisely. Every vote counts!",
            "§c§l⚰ §7The hospital holds many secrets. Explore carefully.",
            "§c§l🩸 §7Blood pacts can save you... or betray you.",
            "§c§l👻 §7The Silent One cannot speak, but they can survive.",
            "§c§l🧙 §7The Wizard seeks soul shards for ultimate power.",
            "§c§l💊 §7The Healer can restore life, but at what cost?",
            "§c§l🎭 §7The Monster can disguise, sabotage, and kill.",
            "§c§l🔍 §7Investigate, deduce, and survive the Blood Ties!"
        ));
    }
    
    private void startAutoStartTimer() {
        if (autoStartTask != null) {
            autoStartTask.cancel();
        }
        
        autoStartTimer = configManager.getLobbyDuration();
        autoStartTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.LOBBY) {
                    autoStartTimer--;
                    
                    // Update boss bar
                    if (gameBossBar != null) {
                        double progress = (double) autoStartTimer / configManager.getLobbyDuration();
                        gameBossBar.setProgress(Math.max(0.0, progress));
                        gameBossBar.setTitle("§c§lBlood Ties §7- §eAuto-start in " + autoStartTimer + "s");
                    }
                    
                    // Broadcast countdown
                    if (autoStartTimer <= 10 && autoStartTimer > 0) {
                        broadcastMessage("§c§l⚡ §7Game starting in §e" + autoStartTimer + "s§7!");
                        soundManager.playSoundToAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                    
                    // Check if enough players
                    if (players.size() >= configManager.getMinPlayers()) {
                        if (autoStartTimer <= 0) {
                            start();
                            cancel();
                        }
                    } else {
                        // Reset timer if not enough players
                        autoStartTimer = configManager.getLobbyDuration();
                        broadcastMessage("§c§l⏰ §7Waiting for players... §e(" + players.size() + "/" + configManager.getMinPlayers() + ")");
                    }
                } else {
                    cancel();
                }
            }
        };
        autoStartTask.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void createBossBar() {
        gameBossBar = Bukkit.createBossBar(
            "§c§lBlood Ties §7- §eWaiting for players...",
            BarColor.RED,
            BarStyle.SOLID
        );
        gameBossBar.setProgress(1.0);
    }
    
    private void createScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        gameScoreboard = manager.getNewScoreboard();
        gameObjective = gameScoreboard.registerNewObjective("bloodties", "dummy", "§c§lBlood Ties");
        gameObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    
    public void start() {
        if (state != GameState.LOBBY) {
            return;
        }
        
        Logger.info("Starting Blood Ties game with " + players.size() + " players in " + mode.getDisplayName() + " mode");
        
        // Cancel auto-start
        if (autoStartTask != null) {
            autoStartTask.cancel();
        }
        
        // Assign roles
        assignRoles();
        
        // Set state
        setState(GameState.STARTING);
        
        // Broadcast start
        broadcastMessage("§c§l🎮 §7Blood Ties has begun! §e" + players.size() + " players §7enter the abandoned hospital...");
        soundManager.playGameStart();
        
        // Teleport to arena
        teleportPlayersToArena();
        
        // Start game loop
        startGameLoop();
        
        // Start ambient sounds
        startAmbientSounds();
        
        // Start particle effects
        startParticleEffects();
        
        // Start broadcast rotation
        startBroadcastRotation();
    }
    
    private void assignRoles() {
        List<UUID> playerList = new ArrayList<>(players.keySet());
        Collections.shuffle(playerList);
        
        // Assign Monster first
        if (!playerList.isEmpty()) {
            UUID monsterId = playerList.remove(0);
            GamePlayer monster = players.get(monsterId);
            monster.setRole(Role.MONSTER);
            alivePlayers.put(monsterId, monster);
            
            Player monsterPlayer = Bukkit.getPlayer(monsterId);
            if (monsterPlayer != null) {
                monsterPlayer.sendMessage("§c§l👹 §7You are the §cMonster§7! Disguise yourself and eliminate all players.");
                monsterPlayer.sendMessage("§7Abilities: §c/sabotage §7- §c/kill §7- §c/disguise");
                soundManager.playMonsterAssign(monsterPlayer);
            }
        }
        
        // Assign other roles
        List<Role> availableRoles = Arrays.asList(Role.HEALER, Role.WIZARD, Role.SILENT_ONE, Role.SURVIVOR);
        int roleIndex = 0;
        
        for (UUID playerId : playerList) {
            GamePlayer player = players.get(playerId);
            Role role = availableRoles.get(roleIndex % availableRoles.size());
            player.setRole(role);
            alivePlayers.put(playerId, player);
            
            Player bukkitPlayer = Bukkit.getPlayer(playerId);
            if (bukkitPlayer != null) {
                String roleMessage = getRoleAssignmentMessage(role);
                bukkitPlayer.sendMessage(roleMessage);
                soundManager.playRoleAssign(bukkitPlayer);
                
                // Give role-specific items
                giveRoleItems(bukkitPlayer, role);
            }
            
            roleIndex++;
        }
    }
    
    private String getRoleAssignmentMessage(Role role) {
        switch (role) {
            case HEALER:
                return "§a§l💊 §7You are the §aHealer§7! Restore players and sense deception.";
            case WIZARD:
                return "§b§l🧙 §7You are the §bWizard§7! Cast spells and collect soul shards.";
            case SILENT_ONE:
                return "§8§l👻 §7You are the §8Silent One§7! Survive unnoticed and win secretly.";
            case SURVIVOR:
                return "§e§l🔍 §7You are a §eSurvivor§7! Investigate and form blood pacts.";
            default:
                return "§7You have been assigned a role.";
        }
    }
    
    private void giveRoleItems(Player player, Role role) {
        player.getInventory().clear();
        
        switch (role) {
            case MONSTER:
                player.getInventory().addItem(createItem(Material.SKELETON_SKULL, "§c§lMonster Mask", "§7Disguise yourself"));
                player.getInventory().addItem(createItem(Material.REDSTONE, "§c§lSabotage Kit", "§7Break lights and doors"));
                player.getInventory().addItem(createItem(Material.DIAMOND_SWORD, "§c§lKilling Blade", "§7Eliminate players"));
                break;
            case HEALER:
                player.getInventory().addItem(createItem(Material.POTION, "§a§lHealing Potion", "§7Restore a player"));
                player.getInventory().addItem(createItem(Material.EMERALD, "§a§lTruth Stone", "§7Sense lies and injuries"));
                break;
            case WIZARD:
                player.getInventory().addItem(createItem(Material.BLAZE_ROD, "§b§lMagic Staff", "§7Cast detection spells"));
                player.getInventory().addItem(createItem(Material.AMETHYST_SHARD, "§b§lSoul Shard", "§7Collect for secret win"));
                break;
            case SILENT_ONE:
                player.getInventory().addItem(createItem(Material.GLASS, "§8§lSilence Charm", "§7Cannot speak but immune to votes"));
                player.getInventory().addItem(createItem(Material.ENDER_PEARL, "§8§lShadow Step", "§7Teleport silently"));
                break;
            case SURVIVOR:
                player.getInventory().addItem(createItem(Material.BOOK, "§e§lInvestigation Journal", "§7Record clues"));
                player.getInventory().addItem(createItem(Material.RED_DYE, "§e§lBlood Pact Seal", "§7Form voting alliances"));
                break;
        }
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private void startGameLoop() {
        if (gameLoop != null) {
            gameLoop.cancel();
        }
        
        gameLoop = new BukkitRunnable() {
            @Override
            public void run() {
                switch (state) {
                    case STARTING:
                        handleStartingPhase();
                        break;
                    case SPAWN:
                        handleSpawnPhase();
                        break;
                    case ACTION:
                        handleActionPhase();
                        break;
                    case VOTING:
                        handleVotingPhase();
                        break;
                    case CONSEQUENCE:
                        handleConsequencePhase();
                        break;
                    case ENDING:
                        handleEndingPhase();
                        break;
                    default:
                        break;
                }
            }
        };
        gameLoop.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void handleStartingPhase() {
        timeLeft = 10; // 10 second countdown
        setState(GameState.SPAWN);
        broadcastMessage("§c§l⚡ §7Game starting in §e10 seconds§7!");
        soundManager.playCountdown();
    }
    
    private void handleSpawnPhase() {
        if (timeLeft <= 0) {
            setState(GameState.ACTION);
            timeLeft = configManager.getRoundDuration();
            broadcastMessage("§c§l🎭 §7Action phase begins! §eUse your abilities wisely§7!");
            soundManager.playActionStart();
            startVoting();
        } else {
            timeLeft--;
            updateScoreboard();
            
            if (timeLeft <= 5 && timeLeft > 0) {
                broadcastMessage("§c§l⏰ §7Spawn phase ending in §e" + timeLeft + "s§7!");
                soundManager.playCountdown();
            }
        }
    }
    
    private void handleActionPhase() {
        if (timeLeft <= 0) {
            setState(GameState.VOTING);
            timeLeft = configManager.getVoteDuration();
            broadcastMessage("§c§l🗳 §7Voting phase begins! §eVote for sacrifice§7!");
            soundManager.playVoteStart();
            openVotingGUI();
        } else {
            timeLeft--;
            updateScoreboard();
            
            // Check win conditions every 30 seconds
            if (timeLeft % 30 == 0) {
                checkWinConditions();
            }
            
            // Ambient effects
            if (timeLeft % 60 == 0) {
                soundManager.playAmbientHorror();
            }
        }
    }
    
    private void handleVotingPhase() {
        if (timeLeft <= 0) {
            setState(GameState.CONSEQUENCE);
            timeLeft = 10;
            processVotes();
        } else {
            timeLeft--;
            updateScoreboard();
            
            if (timeLeft <= 10 && timeLeft > 0) {
                broadcastMessage("§c§l⏰ §7Voting ends in §e" + timeLeft + "s§7!");
                soundManager.playCountdown();
            }
        }
    }
    
    private void handleConsequencePhase() {
        if (timeLeft <= 0) {
            // Check if game should continue
            if (shouldContinueGame()) {
                resetForNewRound();
                setState(GameState.ACTION);
                timeLeft = configManager.getRoundDuration();
                broadcastMessage("§c§l🔄 §7New round begins! §eThe cycle continues...§7!");
                soundManager.playRoundStart();
            } else {
                endGame();
            }
        } else {
            timeLeft--;
            updateScoreboard();
        }
    }
    
    private void handleEndingPhase() {
        // Game ending logic
        setState(GameState.ENDING);
    }
    
    private void startVoting() {
        // Initialize voting
        votes.clear();
        voteOffers.clear();
        broadcastMessage("§c§l🗳 §7Voting system activated! §eUse /bt vote or the GUI§7!");
    }
    
    private void openVotingGUI() {
        for (UUID playerId : alivePlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                new VotingGUI(this).open(player);
            }
        }
    }
    
    private void processVotes() {
        if (votes.isEmpty()) {
            broadcastMessage("§c§l❌ §7No votes cast! §eRandom player will be sacrificed§7!");
            List<UUID> aliveList = new ArrayList<>(alivePlayers.keySet());
            if (!aliveList.isEmpty()) {
                UUID randomPlayer = aliveList.get(new Random().nextInt(aliveList.size()));
                sacrificePlayer(randomPlayer, "§c§l🎲 §7Random sacrifice");
            }
            return;
        }
        
        // Count votes for each player
        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : votes.entrySet()) {
            UUID voterId = entry.getKey();
            int targetHash = entry.getValue();
            
            // Find the player with this hash code
            for (UUID playerId : alivePlayers.keySet()) {
                if (playerId.hashCode() == targetHash) {
                    voteCounts.put(playerId, voteCounts.getOrDefault(playerId, 0) + 1);
                    break;
                }
            }
        }
        
        // Find player with most votes
        UUID mostVoted = null;
        int maxVotes = 0;
        
        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                mostVoted = entry.getKey();
            }
        }
        
        if (mostVoted != null) {
            Player sacrificed = Bukkit.getPlayer(mostVoted);
            if (sacrificed != null) {
                broadcastMessage("§c§l⚰ §7" + sacrificed.getName() + " §7has been sacrificed by vote!");
                sacrificePlayer(mostVoted, "§c§l🗳 §7Sacrificed by vote");
            }
        }
    }
    
    private void sacrificePlayer(UUID playerId, String reason) {
        GamePlayer gamePlayer = alivePlayers.get(playerId);
        if (gamePlayer == null) return;
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            // Visual effects
            player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation(), 50, Material.REDSTONE_BLOCK.createBlockData());
            player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1);
            
            // Sound effects
            soundManager.playSacrifice(player);
            
            // Kill player
            player.setHealth(0.0);
            
            // Broadcast
            broadcastMessage(reason + " §7- §c" + player.getName() + " §7has fallen!");
        }
        
        // Move to dead players
        alivePlayers.remove(playerId);
        deadPlayers.put(playerId, gamePlayer);
        
        // Update karma
        updateKarmaForSacrifice(playerId, gamePlayer.getRole());
        
        // Check win conditions
        checkWinConditions();
    }
    
    private void updateKarmaForSacrifice(UUID playerId, Role role) {
        // This would be implemented based on voting patterns
        // For now, just add some karma for participation
        dataManager.addKarma(playerId, 5);
    }
    
    private boolean shouldContinueGame() {
        // Check if enough players remain
        if (alivePlayers.size() < 2) {
            return false;
        }
        
        // Check if Monster is still alive
        boolean monsterAlive = alivePlayers.values().stream()
            .anyMatch(p -> p.getRole() == Role.MONSTER);
        
        if (!monsterAlive) {
            return false;
        }
        
        return true;
    }
    
    private void resetForNewRound() {
        roundNumber++;
        votes.clear();
        voteOffers.clear();
        
        // Reset cooldowns
        cooldowns.clear();
        
        // Reset Silent One immunity
        silentOneImmunity.clear();
        
        broadcastMessage("§c§l🔄 §7Round " + roundNumber + " §7begins!");
    }
    
    public void endGame() {
        setState(GameState.ENDING);
        
        // Determine winner
        String winner = determineWinner();
        broadcastMessage("§c§l🏆 §7" + winner);
        
        // Save statistics
        saveGameStats();
        
        // Clean up
        cleanup();
        
        // Teleport to lobby
        teleportPlayersToLobby();
        
        setState(GameState.FINISHED);
    }
    
    private String determineWinner() {
        // Check if Monster won
        boolean monsterAlive = alivePlayers.values().stream()
            .anyMatch(p -> p.getRole() == Role.MONSTER);
        
        if (monsterAlive && alivePlayers.size() == 1) {
            return "§c§l👹 Monster wins! §7All players eliminated!";
        }
        
        // Check if Survivors won
        if (!monsterAlive) {
            return "§a§l🏥 Survivors win! §7Monster has been eliminated!";
        }
        
        // Check secret wins
        for (Map.Entry<UUID, Integer> entry : soulShards.entrySet()) {
            if (entry.getValue() >= 3) {
                Player wizard = Bukkit.getPlayer(entry.getKey());
                if (wizard != null) {
                    return "§b§l🧙 Wizard wins! §7" + wizard.getName() + " §7collected 3 soul shards!";
                }
            }
        }
        
        return "§8§l👻 Silent One wins! §7Survived unnoticed!";
    }
    
    private void saveGameStats() {
        for (Map.Entry<UUID, GamePlayer> entry : players.entrySet()) {
            UUID playerId = entry.getKey();
            GamePlayer gamePlayer = entry.getValue();
            
            // Update games played
            dataManager.incrementGamesPlayed(playerId);
            
            // Update role wins
            if (isWinner(playerId, gamePlayer.getRole())) {
                dataManager.incrementGamesWon(playerId);
                dataManager.incrementRoleWins(playerId, gamePlayer.getRole().name().toLowerCase());
            }
            
            // Update kills
            dataManager.incrementKills(playerId, gamePlayer.getKills());
            
            // Update votes
            dataManager.incrementVotes(playerId, gamePlayer.getVotes());
        }
    }
    
    private boolean isWinner(UUID playerId, Role role) {
        // This would check if the player's team won
        // Simplified for now
        return alivePlayers.containsKey(playerId);
    }
    
    private void cleanup() {
        // Cancel tasks
        if (gameLoop != null) {
            gameLoop.cancel();
        }
        if (ambientSounds != null) {
            ambientSounds.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }
        
        // Remove boss bar
        if (gameBossBar != null) {
            gameBossBar.removeAll();
        }
        
        // Clear collections
        players.clear();
        alivePlayers.clear();
        deadPlayers.clear();
        votes.clear();
        voteOffers.clear();
        cooldowns.clear();
        bloodPacts.clear();
        soulShards.clear();
        silentOneImmunity.clear();
    }
    
    private void startAmbientSounds() {
        if (ambientSounds != null) {
            ambientSounds.cancel();
        }
        
        ambientSounds = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ACTION || state == GameState.VOTING) {
                    soundManager.playAmbientHorror();
                }
            }
        };
        ambientSounds.runTaskTimer(plugin, 200L, 200L); // Every 10 seconds
    }
    
    private void startParticleEffects() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ACTION || state == GameState.VOTING) {
                    // Add ambient particles
                    for (UUID playerId : alivePlayers.keySet()) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            // Random particles around players
                            if (Math.random() < 0.1) { // 10% chance
                                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 5, 0.5, 0.5, 0.5, 0.01);
                            }
                        }
                    }
                }
            }
        };
        particleTask.runTaskTimer(plugin, 40L, 40L); // Every 2 seconds
    }
    
    private void startBroadcastRotation() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }
        
        broadcastTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ACTION || state == GameState.VOTING) {
                    if (broadcastIndex < broadcastMessages.size()) {
                        broadcastMessage(broadcastMessages.get(broadcastIndex));
                        broadcastIndex++;
                    } else {
                        broadcastIndex = 0;
                    }
                }
            }
        };
        broadcastTask.runTaskTimer(plugin, 600L, 600L); // Every 30 seconds
    }
    
    private void updateScoreboard() {
        if (gameObjective == null) return;
        
        gameObjective.getScoreboard().resetScores("§1");
        gameObjective.getScoreboard().resetScores("§2");
        gameObjective.getScoreboard().resetScores("§3");
        gameObjective.getScoreboard().resetScores("§4");
        gameObjective.getScoreboard().resetScores("§5");
        gameObjective.getScoreboard().resetScores("§6");
        gameObjective.getScoreboard().resetScores("§7");
        gameObjective.getScoreboard().resetScores("§8");
        gameObjective.getScoreboard().resetScores("§9");
        gameObjective.getScoreboard().resetScores("§a");
        gameObjective.getScoreboard().resetScores("§b");
        gameObjective.getScoreboard().resetScores("§c");
        gameObjective.getScoreboard().resetScores("§d");
        gameObjective.getScoreboard().resetScores("§e");
        gameObjective.getScoreboard().resetScores("§f");
        
        int score = 10;
        
        // Game state
        Score stateScore = gameObjective.getScore("§c§lState: §7" + state.getDisplayName());
        stateScore.setScore(score--);
        
        // Round
        Score roundScore = gameObjective.getScore("§e§lRound: §7" + roundNumber);
        roundScore.setScore(score--);
        
        // Time left
        Score timeScore = gameObjective.getScore("§b§lTime: §7" + timeLeft + "s");
        timeScore.setScore(score--);
        
        // Players alive
        Score aliveScore = gameObjective.getScore("§a§lAlive: §7" + alivePlayers.size());
        aliveScore.setScore(score--);
        
        // Players dead
        Score deadScore = gameObjective.getScore("§c§lDead: §7" + deadPlayers.size());
        deadScore.setScore(score--);
        
        // Mode
        Score modeScore = gameObjective.getScore("§6§lMode: §7" + mode.getDisplayName());
        modeScore.setScore(score--);
        
        // Update for all players
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setScoreboard(gameScoreboard);
            }
        }
    }
    
    public void addPlayer(UUID playerId, String playerName) {
        GamePlayer gamePlayer = new GamePlayer(playerId, playerName);
        players.put(playerId, gamePlayer);
        
        // Add to boss bar
        if (gameBossBar != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                gameBossBar.addPlayer(player);
            }
        }
        
        // Update auto-start timer
        if (players.size() >= configManager.getMinPlayers()) {
            if (autoStartTimer > 10) {
                autoStartTimer = 10; // Quick start when enough players
            }
        }
        
        broadcastMessage("§a§l➕ §7" + playerName + " §7joined the game! §e(" + players.size() + "/" + configManager.getMaxPlayers() + ")");
        soundManager.playPlayerJoin();
    }
    
    public void removePlayer(UUID playerId) {
        GamePlayer gamePlayer = players.get(playerId);
        if (gamePlayer != null) {
            String playerName = gamePlayer.getName();
            
            players.remove(playerId);
            alivePlayers.remove(playerId);
            deadPlayers.remove(playerId);
            votes.remove(playerId);
            voteOffers.remove(playerId);
            cooldowns.remove(playerId);
            bloodPacts.remove(playerId);
            soulShards.remove(playerId);
            silentOneImmunity.remove(playerId);
            
            // Remove from boss bar
            if (gameBossBar != null) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    gameBossBar.removePlayer(player);
                }
            }
            
            broadcastMessage("§c§l➖ §7" + playerName + " §7left the game!");
            soundManager.playPlayerLeave();
            
            // Check if game should end
            if (players.size() < 2 && state != GameState.LOBBY) {
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
    
    public void castVote(UUID voterId, UUID targetId) {
        if (state != GameState.VOTING) {
            Player voter = Bukkit.getPlayer(voterId);
            if (voter != null) {
                voter.sendMessage("§c§l❌ §7Voting is not currently active!");
            }
            return;
        }
        
        if (!alivePlayers.containsKey(voterId)) {
            Player voter = Bukkit.getPlayer(voterId);
            if (voter != null) {
                voter.sendMessage("§c§l❌ §7You cannot vote while dead!");
            }
            return;
        }
        
        if (!alivePlayers.containsKey(targetId)) {
            Player voter = Bukkit.getPlayer(voterId);
            if (voter != null) {
                voter.sendMessage("§c§l❌ §7You cannot vote for a dead player!");
            }
            return;
        }
        
        votes.put(voterId, targetId.hashCode());
        
        Player voter = Bukkit.getPlayer(voterId);
        Player target = Bukkit.getPlayer(targetId);
        
        if (voter != null && target != null) {
            voter.sendMessage("§a§l✅ §7You voted for §e" + target.getName() + "§7!");
            soundManager.playVoteCast(voter);
        }
    }
    
    public void offerVote(UUID sellerId, String info) {
        if (state != GameState.VOTING) {
            Player seller = Bukkit.getPlayer(sellerId);
            if (seller != null) {
                seller.sendMessage("§c§l❌ §7Voting is not currently active!");
            }
            return;
        }
        
        voteOffers.put(sellerId, info);
        
        Player seller = Bukkit.getPlayer(sellerId);
        if (seller != null) {
            seller.sendMessage("§a§l💰 §7Your vote is now for sale: §e" + info);
            broadcastMessage("§a§l💰 §7" + seller.getName() + " §7is selling their vote: §e" + info);
        }
    }
    
    public void acceptVoteOffer(UUID buyerId, UUID sellerId) {
        if (!voteOffers.containsKey(sellerId)) {
            Player buyer = Bukkit.getPlayer(buyerId);
            if (buyer != null) {
                buyer.sendMessage("§c§l❌ §7That vote offer is no longer available!");
            }
            return;
        }
        
        String info = voteOffers.get(sellerId);
        votes.put(sellerId, buyerId.hashCode()); // Seller votes for buyer
        
        Player buyer = Bukkit.getPlayer(buyerId);
        Player seller = Bukkit.getPlayer(sellerId);
        
        if (buyer != null) {
            buyer.sendMessage("§a§l✅ §7You bought " + seller.getName() + "'s vote: §e" + info);
        }
        if (seller != null) {
            seller.sendMessage("§a§l💰 §7" + buyer.getName() + " §7bought your vote!");
        }
        
        voteOffers.remove(sellerId);
    }
    
    public void sendWhisper(UUID senderId, UUID receiverId, String message) {
        Player sender = Bukkit.getPlayer(senderId);
        Player receiver = Bukkit.getPlayer(receiverId);
        
        if (sender == null || receiver == null) {
            return;
        }
        
        // Check if Silent One is trying to speak
        GamePlayer senderGame = getPlayer(senderId);
        if (senderGame != null && senderGame.getRole() == Role.SILENT_ONE) {
            sender.sendMessage("§c§l🤐 §7You cannot speak as the Silent One!");
            return;
        }
        
        sender.sendMessage("§8§l[To " + receiver.getName() + "] §7" + message);
        receiver.sendMessage("§8§l[From " + sender.getName() + "] §7" + message);
        
        soundManager.playWhisper(sender);
        soundManager.playWhisper(receiver);
    }
    
    public void broadcastMessage(String message) {
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(message);
            }
        }
        
        // Also log to console
        Logger.info("[Broadcast] " + message.replaceAll("§[0-9a-fk-or]", ""));
    }
    
    public void setState(GameState newState) {
        this.state = newState;
        
        // Update boss bar title
        if (gameBossBar != null) {
            gameBossBar.setTitle("§c§lBlood Ties §7- §e" + newState.getDisplayName());
        }
        
        // Update scoreboard
        updateScoreboard();
    }
    
    private void teleportPlayersToArena() {
        // This would teleport players to the arena location
        // For now, just broadcast
        broadcastMessage("§c§l🏥 §7Players have been transported to the abandoned hospital!");
    }
    
    private void teleportPlayersToLobby() {
        // This would teleport players back to lobby
        broadcastMessage("§a§l🏠 §7Players have been returned to the lobby!");
    }
    
    public void checkWinConditions() {
        // Check if Monster won
        boolean monsterAlive = alivePlayers.values().stream()
            .anyMatch(p -> p.getRole() == Role.MONSTER);
        
        if (monsterAlive && alivePlayers.size() == 1) {
            endGame();
            return;
        }
        
        // Check if Survivors won
        if (!monsterAlive) {
            endGame();
            return;
        }
        
        // Check secret wins
        for (Map.Entry<UUID, Integer> entry : soulShards.entrySet()) {
            if (entry.getValue() >= 3) {
                endGame();
                return;
            }
        }
    }
    
    // Getters for other classes
    public GameState getState() {
        return state;
    }
    
    public GameMode getMode() {
        return mode;
    }
    
    public Map<UUID, GamePlayer> getPlayers() {
        return players;
    }
    
    public Map<UUID, GamePlayer> getAlivePlayers() {
        return alivePlayers;
    }
    
    public Map<UUID, GamePlayer> getDeadPlayers() {
        return deadPlayers;
    }
    
    public Map<UUID, Integer> getVotes() {
        return votes;
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public int getTimeLeft() {
        return timeLeft;
    }
    
    public BossBar getBossBar() {
        return gameBossBar;
    }
    
    public Scoreboard getScoreboard() {
        return gameScoreboard;
    }
}