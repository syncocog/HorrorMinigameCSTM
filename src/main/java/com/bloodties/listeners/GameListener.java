package com.bloodties.listeners;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.game.Game;
import com.bloodties.game.GamePlayer;
import com.bloodties.game.GameState;
import com.bloodties.gui.VotingGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class GameListener implements Listener {
    
    private final BloodTiesPlugin plugin;
    
    public GameListener(BloodTiesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive()) {
                // Handle player death in game
                gamePlayer.setAlive(false);
                game.getAlivePlayers().remove(player.getUniqueId());
                game.getDeadPlayers().put(player.getUniqueId(), gamePlayer);
                
                // Check if Monster killed them
                Player killer = player.getKiller();
                if (killer != null) {
                    GamePlayer killerGamePlayer = game.getPlayer(killer.getUniqueId());
                    if (killerGamePlayer != null && killerGamePlayer.isMonster()) {
                        killerGamePlayer.incrementKills();
                        killerGamePlayer.setLastKillTime(System.currentTimeMillis());
                        
                        game.broadcastMessage(ChatColor.DARK_RED + "☠ " + player.getName() + 
                            " was killed by the Monster!");
                        plugin.getSoundManager().playMonsterKill(player.getLocation());
                    }
                }
                
                // Check win conditions
                if (game.checkWinConditions()) {
                    game.setState(GameState.ENDING);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive()) {
                // Allow damage in games
                return;
            }
        }
        
        // Prevent damage outside of games or for dead players
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        
        Player damager = (Player) event.getDamager();
        Player target = (Player) event.getEntity();
        
        Game game = plugin.getGameManager().getPlayerGame(damager);
        if (game != null && game.getState().isActive()) {
            GamePlayer damagerGamePlayer = game.getPlayer(damager.getUniqueId());
            GamePlayer targetGamePlayer = game.getPlayer(target.getUniqueId());
            
            if (damagerGamePlayer != null && targetGamePlayer != null && 
                damagerGamePlayer.isAlive() && targetGamePlayer.isAlive()) {
                
                // Only allow Monster to damage other players
                if (damagerGamePlayer.isMonster()) {
                    // Monster can kill with any weapon
                    return;
                } else {
                    // Other players can't damage each other
                    event.setCancelled(true);
                    damager.sendMessage(ChatColor.RED + "You cannot attack other players!");
                }
            }
        } else {
            // Prevent PvP outside of games
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive()) {
                // Allow block breaking in games (for Monster sabotage)
                if (gamePlayer.isMonster()) {
                    // Monster can break blocks for sabotage
                    plugin.getSoundManager().playSabotage(event.getBlock().getLocation());
                    return;
                }
            }
        }
        
        // Prevent block breaking outside of games or for non-Monsters
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive()) {
                // Allow block placing in games
                return;
            }
        }
        
        // Prevent block placing outside of games
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive()) {
                // Allow interactions in games
                return;
            }
        }
        
        // Prevent interactions outside of games
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Handle voting GUI
        if (title.equals(ChatColor.DARK_RED + "Vote for the Monster")) {
            event.setCancelled(true);
            
            Game game = plugin.getGameManager().getPlayerGame(player);
            if (game != null && game.getState() == GameState.VOTING) {
                VotingGUI votingGUI = game.getVotingGUI();
                votingGUI.handleClick(player, event.getRawSlot());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive()) {
                // Check for Silent One movement restrictions
                if (gamePlayer.isSilentOne()) {
                    // Silent One might have movement restrictions
                    // This could be implemented for special mechanics
                }
                
                // Check for Monster proximity effects
                if (game.getMonster() != null && game.getMonster().isAlive()) {
                    Player monsterPlayer = plugin.getServer().getPlayer(game.getMonster().getPlayerId());
                    if (monsterPlayer != null && player.getLocation().distance(monsterPlayer.getLocation()) < 10) {
                        // Player is near Monster - could trigger effects
                        plugin.getSoundManager().playHeartbeat(player);
                    }
                }
            }
        }
    }
    
    // Prevent item dropping in games
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive()) {
                // Allow item dropping in games
                return;
            }
        }
        
        // Prevent item dropping outside of games
        event.setCancelled(true);
    }
    
    // Handle chat restrictions for Silent One
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive() && gamePlayer.isSilentOne()) {
                // Silent One cannot speak in public chat
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You are the Silent One and cannot speak!");
                plugin.getSoundManager().playCooldown(player);
            }
        }
    }
    
    // Handle food level changes
    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game != null && game.getState().isActive()) {
            GamePlayer gamePlayer = game.getPlayer(player.getUniqueId());
            if (gamePlayer != null && gamePlayer.isAlive()) {
                // Allow hunger in games
                return;
            }
        }
        
        // Prevent hunger outside of games
        event.setCancelled(true);
    }
    
    // Handle weather changes
    @EventHandler(priority = EventPriority.HIGH)
    public void onWeatherChange(org.bukkit.event.weather.WeatherChangeEvent event) {
        // Prevent weather changes during games for atmosphere
        if (plugin.getGameManager().getActiveGamesCount() > 0) {
            event.setCancelled(true);
        }
    }
    
    // Handle time changes - removed due to API compatibility
    // Time changes can be handled through other means if needed
}