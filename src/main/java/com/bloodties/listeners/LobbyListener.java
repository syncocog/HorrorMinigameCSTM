package com.bloodties.listeners;

import com.bloodties.BloodTiesPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class LobbyListener implements Listener {
    
    private final BloodTiesPlugin plugin;
    
    public LobbyListener(BloodTiesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Handle player join
        plugin.getGameManager().handlePlayerJoin(player);
        
        // Send welcome message
        player.sendMessage(ChatColor.DARK_RED + "╔══════════════════════════════════════╗");
        player.sendMessage(ChatColor.DARK_RED + "║           " + ChatColor.WHITE + "BLOOD TIES" + ChatColor.DARK_RED + "           ║");
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        player.sendMessage(ChatColor.WHITE + "Welcome to Blood Ties!");
        player.sendMessage(ChatColor.GRAY + "A social deduction minigame where");
        player.sendMessage(ChatColor.GRAY + "you must survive and identify the Monster.");
        player.sendMessage(ChatColor.DARK_RED + "╠══════════════════════════════════════╣");
        player.sendMessage(ChatColor.YELLOW + "Use /bt join <mode> to start playing!");
        player.sendMessage(ChatColor.YELLOW + "Use /bt help for more commands.");
        player.sendMessage(ChatColor.DARK_RED + "╚══════════════════════════════════════╝");
        
        // Update player name in data
        plugin.getDataManager().updatePlayerName(player.getUniqueId(), player.getName());
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Handle player quit
        plugin.getGameManager().handlePlayerQuit(player);
        
        // Clear heartbeat timer
        plugin.getSoundManager().clearHeartbeatTimer(player.getUniqueId());
    }
}