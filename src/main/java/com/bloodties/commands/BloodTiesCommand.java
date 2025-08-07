package com.bloodties.commands;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.game.Game;
import com.bloodties.game.GameMode;
import com.bloodties.game.GamePlayer;
import com.bloodties.game.Role;
import com.bloodties.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BloodTiesCommand implements CommandExecutor, TabCompleter {
    
    private final BloodTiesPlugin plugin;
    
    public BloodTiesCommand(BloodTiesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "join":
                handleJoin(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "vote":
                handleVote(player, args);
                break;
            case "sellvote":
                handleSellVote(player, args);
                break;
            case "useability":
                handleUseAbility(player);
                break;
            case "stats":
                handleStats(player);
                break;
            case "roles":
                handleRoles(player);
                break;
            case "whisper":
                handleWhisper(player, args);
                break;
            case "map":
                handleMap(player);
                break;
            case "team":
                handleTeam(player);
                break;
            case "admin":
                handleAdmin(player, args);
                break;
            case "help":
                sendHelpMessage(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /bt help for help.");
                break;
        }
        
        return true;
    }
    
    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bt join <mode>");
            player.sendMessage(ChatColor.GRAY + "Available modes: solo, duo, squad");
            return;
        }
        
        String modeName = args[1].toLowerCase();
        GameMode mode;
        
        switch (modeName) {
            case "solo":
                mode = GameMode.SOLO;
                break;
            case "duo":
                mode = GameMode.DUO;
                break;
            case "squad":
                mode = GameMode.SQUAD;
                break;
            default:
                player.sendMessage(ChatColor.RED + "Invalid game mode. Use: solo, duo, or squad");
                return;
        }
        
        plugin.getGameManager().addToQueue(player, mode);
    }
    
    private void handleLeave(Player player) {
        if (plugin.getGameManager().isPlayerInGame(player)) {
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game != null) {
                game.removePlayer(player);
            }
            player.sendMessage(ChatColor.GREEN + "You have left the game!");
        } else if (plugin.getGameManager().isPlayerInQueue(player)) {
            plugin.getGameManager().removeFromQueue(player);
        } else {
            player.sendMessage(ChatColor.RED + "You are not in a game or queue!");
        }
    }
    
    private void handleVote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bt vote <player>");
            return;
        }
        
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return;
        }
        
        if (game.getState() != com.bloodties.game.GameState.VOTING) {
            player.sendMessage(ChatColor.RED + "Voting is not currently active!");
            return;
        }
        
        String targetName = args[1];
        game.castVote(player.getUniqueId(), targetName);
    }
    
    private void handleSellVote(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /bt sellvote <player> <info>");
            return;
        }
        
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return;
        }
        
        String targetName = args[1];
        String info = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        game.offerVote(player.getUniqueId(), targetName, info);
        player.sendMessage(ChatColor.YELLOW + "Vote offer sent for " + targetName + " with info: " + info);
    }
    
    private void handleUseAbility(Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return;
        }
        
        GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
        if (gamePlayer == null) {
            player.sendMessage(ChatColor.RED + "Player data not found!");
            return;
        }
        
        if (!gamePlayer.canUseAbility()) {
            long remaining = gamePlayer.getRemainingCooldown();
            player.sendMessage(ChatColor.RED + "Ability on cooldown! " + (remaining / 1000) + "s remaining");
            plugin.getSoundManager().playCooldown(player);
            return;
        }
        
        // Handle different role abilities
        switch (gamePlayer.getRole()) {
            case MONSTER:
                handleMonsterAbility(player, gamePlayer);
                break;
            case HEALER:
                handleHealerAbility(player, gamePlayer);
                break;
            case WIZARD:
                handleWizardAbility(player, gamePlayer);
                break;
            case SILENT_ONE:
                handleSilentOneAbility(player, gamePlayer);
                break;
            case SURVIVOR:
                handleSurvivorAbility(player, gamePlayer);
                break;
        }
    }
    
    private void handleMonsterAbility(Player player, GamePlayer gamePlayer) {
        // Monster can kill nearby players
        if (!gamePlayer.canKill()) {
            long remaining = gamePlayer.getRemainingCooldown("kill");
            player.sendMessage(ChatColor.RED + "Kill ability on cooldown! " + (remaining / 1000) + "s remaining");
            return;
        }
        
        // Find nearby players to kill
        List<Player> nearbyPlayers = player.getNearbyEntities(5, 5, 5).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .collect(Collectors.toList());
        
        if (nearbyPlayers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No players nearby to kill!");
            return;
        }
        
        // Kill the closest player
        Player target = nearbyPlayers.get(0);
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            GamePlayer targetGamePlayer = game.getPlayer(target.getUniqueId());
            if (targetGamePlayer != null && targetGamePlayer.isAlive()) {
                target.setHealth(0);
                gamePlayer.setLastKillTime(System.currentTimeMillis());
                gamePlayer.incrementKills();
                
                player.sendMessage(ChatColor.DARK_RED + "You killed " + target.getName() + "!");
                plugin.getSoundManager().playMonsterKill(target.getLocation());
            }
        }
    }
    
    private void handleHealerAbility(Player player, GamePlayer gamePlayer) {
        if (gamePlayer.hasUsedHeal()) {
            player.sendMessage(ChatColor.RED + "You have already used your heal ability this game!");
            return;
        }
        
        // Find nearby injured players
        List<Player> nearbyPlayers = player.getNearbyEntities(10, 10, 10).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .filter(p -> p.getHealth() < p.getMaxHealth())
            .collect(Collectors.toList());
        
        if (nearbyPlayers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No injured players nearby!");
            return;
        }
        
        // Heal the most injured player
        Player target = nearbyPlayers.stream()
            .min((p1, p2) -> Double.compare(p1.getHealth(), p2.getHealth()))
            .orElse(null);
        
        if (target != null) {
            target.setHealth(target.getMaxHealth());
            gamePlayer.setHasUsedHeal(true);
            
            player.sendMessage(ChatColor.GREEN + "You healed " + target.getName() + "!");
            target.sendMessage(ChatColor.GREEN + "You have been healed by " + player.getName() + "!");
            plugin.getSoundManager().playHeal(target.getLocation());
        }
    }
    
    private void handleWizardAbility(Player player, GamePlayer gamePlayer) {
        if (gamePlayer.isOnCooldown("spell")) {
            long remaining = gamePlayer.getRemainingCooldown("spell");
            player.sendMessage(ChatColor.RED + "Spell on cooldown! " + (remaining / 1000) + "s remaining");
            return;
        }
        
        // Wizard can see roles of nearby players
        List<Player> nearbyPlayers = player.getNearbyEntities(15, 15, 15).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .collect(Collectors.toList());
        
        if (nearbyPlayers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No players nearby to scan!");
            return;
        }
        
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game != null) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "=== Spectral Vision ===");
            for (Player nearby : nearbyPlayers) {
                GamePlayer nearbyGamePlayer = game.getPlayer(nearby.getUniqueId());
                if (nearbyGamePlayer != null) {
                    player.sendMessage(ChatColor.GRAY + nearby.getName() + ": " + 
                        nearbyGamePlayer.getDisplayRoleName());
                }
            }
            
            gamePlayer.setCooldown("spell", 120000); // 2 minutes
            plugin.getSoundManager().playWizardSpell(player.getLocation());
        }
    }
    
    private void handleSilentOneAbility(Player player, GamePlayer gamePlayer) {
        if (gamePlayer.hasUsedVoteImmunity()) {
            player.sendMessage(ChatColor.RED + "You have already used your vote immunity this game!");
            return;
        }
        
        gamePlayer.setHasUsedVoteImmunity(true);
        player.sendMessage(ChatColor.GREEN + "You are now immune to votes for this round!");
    }
    
    private void handleSurvivorAbility(Player player, GamePlayer gamePlayer) {
        // Survivors can investigate areas for clues
        player.sendMessage(ChatColor.YELLOW + "You investigate the area...");
        player.sendMessage(ChatColor.GRAY + "You find some blood stains and broken glass.");
        player.sendMessage(ChatColor.GRAY + "The Monster was here recently...");
    }
    
    private void handleStats(Player player) {
        DataManager dataManager = plugin.getDataManager();
        var playerData = dataManager.getPlayerData(player);
        
        player.sendMessage(ChatColor.DARK_RED + "╔══════════════════════════════════════╗");
        player.sendMessage(ChatColor.DARK_RED + "║           " + ChatColor.WHITE + "YOUR STATS" + ChatColor.DARK_RED + "            ║");
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        player.sendMessage(ChatColor.WHITE + "Karma: " + ChatColor.GOLD + playerData.getKarma());
        player.sendMessage(ChatColor.WHITE + "Games Played: " + ChatColor.AQUA + playerData.getGamesPlayed());
        player.sendMessage(ChatColor.WHITE + "Games Won: " + ChatColor.GREEN + playerData.getGamesWon());
        player.sendMessage(ChatColor.WHITE + "Win Rate: " + ChatColor.YELLOW + 
            String.format("%.1f%%", playerData.getWinRate() * 100));
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        player.sendMessage(ChatColor.WHITE + "Role Wins:");
        player.sendMessage(ChatColor.GRAY + "  Monster: " + ChatColor.RED + playerData.getMonsterWins());
        player.sendMessage(ChatColor.GRAY + "  Healer: " + ChatColor.GREEN + playerData.getHealerWins());
        player.sendMessage(ChatColor.GRAY + "  Wizard: " + ChatColor.LIGHT_PURPLE + playerData.getWizardWins());
        player.sendMessage(ChatColor.GRAY + "  Silent One: " + ChatColor.GRAY + playerData.getSilentOneWins());
        player.sendMessage(ChatColor.GRAY + "  Survivor: " + ChatColor.WHITE + playerData.getSurvivorWins());
        player.sendMessage(ChatColor.DARK_RED + "╚══════════════════════════════════════╝");
    }
    
    private void handleRoles(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "╔══════════════════════════════════════╗");
        player.sendMessage(ChatColor.DARK_RED + "║           " + ChatColor.WHITE + "GAME ROLES" + ChatColor.DARK_RED + "           ║");
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        
        for (Role role : Role.values()) {
            player.sendMessage(ChatColor.DARK_RED + "║  " + role.getColoredName() + ChatColor.DARK_RED + "  ║");
            player.sendMessage(ChatColor.WHITE + role.getDescription());
            player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        }
        
        player.sendMessage(ChatColor.DARK_RED + "╚══════════════════════════════════════╝");
    }
    
    private void handleWhisper(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /bt whisper <player> <message>");
            return;
        }
        
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return;
        }
        
        String targetName = args[1];
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }
        
        Game targetGame = plugin.getGameManager().getPlayerGame(target);
        if (targetGame != game) {
            player.sendMessage(ChatColor.RED + "That player is not in your game!");
            return;
        }
        
        game.sendWhisper(player.getUniqueId(), target.getUniqueId(), message);
    }
    
    private void handleMap(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Map feature not yet implemented!");
    }
    
    private void handleTeam(Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "You are not in a game!");
            return;
        }
        
        if (game.getGameMode().isSolo()) {
            player.sendMessage(ChatColor.RED + "Teams are not available in Solo mode!");
            return;
        }
        
        GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
        if (gamePlayer == null) {
            player.sendMessage(ChatColor.RED + "Player data not found!");
            return;
        }
        
        if (gamePlayer.getBloodPactPartner() != null) {
            player.sendMessage(ChatColor.GREEN + "Your blood pact partner: " + 
                gamePlayer.getBloodPactPartner().getName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "You don't have a blood pact partner yet.");
        }
    }
    
    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("bloodties.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use admin commands!");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bt admin <subcommand>");
            player.sendMessage(ChatColor.GRAY + "Subcommands: forcestart, stopall, kick, stats");
            return;
        }
        
        String adminCommand = args[1].toLowerCase();
        
        switch (adminCommand) {
            case "forcestart":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /bt admin forcestart <mode>");
                    return;
                }
                String modeName = args[2].toLowerCase();
                GameMode mode;
                switch (modeName) {
                    case "solo": mode = GameMode.SOLO; break;
                    case "duo": mode = GameMode.DUO; break;
                    case "squad": mode = GameMode.SQUAD; break;
                    default:
                        player.sendMessage(ChatColor.RED + "Invalid mode!");
                        return;
                }
                plugin.getGameManager().forceStartGame(mode);
                player.sendMessage(ChatColor.GREEN + "Force started " + mode.getDisplayName() + " game!");
                break;
                
            case "stopall":
                plugin.getGameManager().stopAllGames();
                player.sendMessage(ChatColor.GREEN + "Stopped all games!");
                break;
                
            case "kick":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /bt admin kick <player>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found!");
                    return;
                }
                plugin.getGameManager().kickPlayerFromGame(target);
                player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + " from their game!");
                break;
                
            case "stats":
                player.sendMessage(ChatColor.GREEN + "=== Blood Ties Statistics ===");
                player.sendMessage(ChatColor.WHITE + "Active Games: " + 
                    ChatColor.AQUA + plugin.getGameManager().getActiveGamesCount());
                player.sendMessage(ChatColor.WHITE + "Players in Games: " + 
                    ChatColor.AQUA + plugin.getGameManager().getTotalPlayersInGames());
                for (GameMode gameMode : GameMode.values()) {
                    player.sendMessage(ChatColor.WHITE + gameMode.getDisplayName() + " Queue: " + 
                        ChatColor.AQUA + plugin.getGameManager().getQueueSize(gameMode));
                }
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "Unknown admin command!");
                break;
        }
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "╔══════════════════════════════════════╗");
        player.sendMessage(ChatColor.DARK_RED + "║           " + ChatColor.WHITE + "BLOOD TIES HELP" + ChatColor.DARK_RED + "        ║");
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        player.sendMessage(ChatColor.WHITE + "/bt join <mode> " + ChatColor.GRAY + "- Join a game");
        player.sendMessage(ChatColor.WHITE + "/bt leave " + ChatColor.GRAY + "- Leave game/queue");
        player.sendMessage(ChatColor.WHITE + "/bt vote <player> " + ChatColor.GRAY + "- Vote for player");
        player.sendMessage(ChatColor.WHITE + "/bt sellvote <player> <info> " + ChatColor.GRAY + "- Trade vote");
        player.sendMessage(ChatColor.WHITE + "/bt useability " + ChatColor.GRAY + "- Use role ability");
        player.sendMessage(ChatColor.WHITE + "/bt stats " + ChatColor.GRAY + "- View your statistics");
        player.sendMessage(ChatColor.WHITE + "/bt roles " + ChatColor.GRAY + "- View role descriptions");
        player.sendMessage(ChatColor.WHITE + "/bt whisper <player> <msg> " + ChatColor.GRAY + "- Private chat");
        player.sendMessage(ChatColor.WHITE + "/bt team " + ChatColor.GRAY + "- View team info (Duo/Squad)");
        player.sendMessage(ChatColor.DARK_RED + "╚══════════════════════════════════════╝");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "join", "leave", "vote", "sellvote", "useability", 
                "stats", "roles", "whisper", "map", "team", "help"
            );
            
            if (sender.hasPermission("bloodties.admin")) {
                subCommands.add("admin");
            }
            
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join":
                    completions.addAll(Arrays.asList("solo", "duo", "squad"));
                    break;
                case "vote":
                case "sellvote":
                case "whisper":
                    // Add online players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "admin":
                    if (sender.hasPermission("bloodties.admin")) {
                        completions.addAll(Arrays.asList("forcestart", "stopall", "kick", "stats"));
                    }
                    break;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("forcestart")) {
                completions.addAll(Arrays.asList("solo", "duo", "squad"));
            } else if (args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("kick")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}