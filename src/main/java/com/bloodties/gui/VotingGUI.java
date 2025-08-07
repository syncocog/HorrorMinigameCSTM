package com.bloodties.gui;

import com.bloodties.game.Game;
import com.bloodties.game.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class VotingGUI {
    
    private final Game game;
    private static final String GUI_TITLE = ChatColor.DARK_RED + "Vote for the Monster";
    
    public VotingGUI(Game game) {
        this.game = game;
    }
    
    public void open(Player player) {
        Inventory gui = createVotingGUI(player);
        player.openInventory(gui);
    }
    
    private Inventory createVotingGUI(Player player) {
        List<GamePlayer> alivePlayers = new ArrayList<>(game.getAlivePlayers().values());
        
        // Calculate inventory size (multiple of 9, minimum 27)
        int size = Math.max(27, ((alivePlayers.size() + 8) / 9) * 9);
        Inventory gui = Bukkit.createInventory(null, size, GUI_TITLE);
        
        // Add player heads
        for (int i = 0; i < alivePlayers.size(); i++) {
            GamePlayer gamePlayer = alivePlayers.get(i);
            ItemStack playerHead = createPlayerHead(gamePlayer, player);
            gui.setItem(i, playerHead);
        }
        
        // Add skip vote option
        ItemStack skipVote = createSkipVoteItem();
        gui.setItem(size - 5, skipVote);
        
        // Add info items
        ItemStack info = createInfoItem();
        gui.setItem(size - 9, info);
        
        return gui;
    }
    
    private ItemStack createPlayerHead(GamePlayer gamePlayer, Player voter) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + gamePlayer.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to vote for this player");
            lore.add("");
            
            // Show role if voter is Wizard or if player is dead
            if (voter.getUniqueId().equals(gamePlayer.getPlayerId())) {
                lore.add(ChatColor.RED + "You cannot vote for yourself!");
            } else {
                GamePlayer voterGamePlayer = game.getPlayer(voter.getUniqueId());
                if (voterGamePlayer != null && voterGamePlayer.isWizard()) {
                    lore.add(ChatColor.LIGHT_PURPLE + "Role: " + gamePlayer.getDisplayRoleName());
                }
                
                // Show status
                lore.add(gamePlayer.getStatusMessage());
                
                // Show vote count if available
                if (voterGamePlayer != null && voterGamePlayer.isWizard()) {
                    int voteCount = getVoteCount(gamePlayer.getName());
                    lore.add(ChatColor.YELLOW + "Current votes: " + voteCount);
                }
            }
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    private ItemStack createSkipVoteItem() {
        ItemStack skip = new ItemStack(Material.BARRIER);
        ItemMeta meta = skip.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Skip Vote");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to skip voting this round");
            lore.add(ChatColor.YELLOW + "Use this if you're unsure");
            
            meta.setLore(lore);
            skip.setItemMeta(meta);
        }
        
        return skip;
    }
    
    private ItemStack createInfoItem() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Voting Information");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Round: " + ChatColor.WHITE + game.getRound());
            lore.add(ChatColor.GRAY + "Time remaining: " + ChatColor.WHITE + game.getTimeRemaining() + "s");
            lore.add(ChatColor.GRAY + "Alive players: " + ChatColor.WHITE + game.getAlivePlayers().size());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Vote for who you think");
            lore.add(ChatColor.YELLOW + "is the Monster!");
            
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        
        return info;
    }
    
    private int getVoteCount(String playerName) {
        int count = 0;
        for (String vote : game.getVotes().values()) {
            if (vote.equals(playerName)) {
                count++;
            }
        }
        return count;
    }
    
    public void handleClick(Player player, int slot) {
        List<GamePlayer> alivePlayers = new ArrayList<>(game.getAlivePlayers().values());
        
        if (slot < alivePlayers.size()) {
            // Player clicked on a player head
            GamePlayer target = alivePlayers.get(slot);
            
            if (target.getPlayerId().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot vote for yourself!");
                return;
            }
            
            // Cast vote
            game.castVote(player.getUniqueId(), target.getName());
            player.closeInventory();
            
        } else if (slot == player.getOpenInventory().getTopInventory().getSize() - 5) {
            // Player clicked skip vote
            player.sendMessage(ChatColor.YELLOW + "You chose to skip voting this round.");
            player.closeInventory();
        }
    }
    
    public void updateGUI() {
        // Update GUI for all players who have it open
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
                Inventory newGUI = createVotingGUI(player);
                player.openInventory(newGUI);
            }
        }
    }
}