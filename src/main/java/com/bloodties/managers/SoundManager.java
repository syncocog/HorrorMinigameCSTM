package com.bloodties.managers;

import com.bloodties.BloodTiesPlugin;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoundManager {
    
    private final BloodTiesPlugin plugin;
    private final Map<UUID, Long> lastHeartbeatTime;
    
    public SoundManager(BloodTiesPlugin plugin) {
        this.plugin = plugin;
        this.lastHeartbeatTime = new HashMap<>();
    }
    
    public void playSound(Player player, Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        player.playSound(player.getLocation(), sound, 
            SoundCategory.MASTER, 
            volume * (float) plugin.getConfigManager().getSoundVolume(),
            pitch * (float) plugin.getConfigManager().getSoundPitch());
    }
    
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        location.getWorld().playSound(location, sound, 
            SoundCategory.MASTER,
            volume * (float) plugin.getConfigManager().getSoundVolume(),
            pitch * (float) plugin.getConfigManager().getSoundPitch());
    }
    
    public void playSoundToAll(Location location, Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        location.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(location) <= 50) {
                playSound(player, sound, volume, pitch);
            }
        });
    }
    
    // Game-specific sounds
    public void playGameStart(Player player) {
        playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        playSound(player, Sound.BLOCK_BELL_USE, 0.8f, 1.2f);
    }
    
    public void playVoteStart(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f);
    }
    
    public void playVoteEnd(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.8f);
    }
    
    public void playSacrifice(Location location) {
        playSoundToAll(location, Sound.ENTITY_PLAYER_DEATH, 1.0f, 0.7f);
        playSoundToAll(location, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
        playSoundToAll(location, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);
    }
    
    public void playMonsterKill(Location location) {
        playSoundToAll(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        playSoundToAll(location, Sound.ENTITY_PLAYER_DEATH, 0.8f, 0.8f);
        playSoundToAll(location, Sound.ENTITY_GHAST_SCREAM, 0.5f, 1.5f);
    }
    
    public void playHeal(Location location) {
        playSoundToAll(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        playSoundToAll(location, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
    }
    
    public void playWizardSpell(Location location) {
        playSoundToAll(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        playSoundToAll(location, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);
    }
    
    public void playSabotage(Location location) {
        playSoundToAll(location, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        playSoundToAll(location, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.8f, 1.0f);
    }
    
    public void playHeartbeat(Player player) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastHeartbeatTime.getOrDefault(player.getUniqueId(), 0L);
        
        // Only play heartbeat every 3 seconds
        if (currentTime - lastTime > 3000) {
            playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.5f);
            lastHeartbeatTime.put(player.getUniqueId(), currentTime);
        }
    }
    
    public void playAmbientHorror(Location location) {
        // Random ambient sounds
        double random = Math.random();
        if (random < 0.1) {
            playSoundToAll(location, Sound.ENTITY_CAT_HISS, 0.3f, 0.8f);
        } else if (random < 0.2) {
            playSoundToAll(location, Sound.BLOCK_WOOD_BREAK, 0.4f, 0.7f);
        } else if (random < 0.3) {
            playSoundToAll(location, Sound.ENTITY_BAT_AMBIENT, 0.3f, 1.2f);
        } else if (random < 0.4) {
            playSoundToAll(location, Sound.BLOCK_GLASS_BREAK, 0.2f, 0.6f);
        }
    }
    
    public void playLightsFlicker(Location location) {
        playSoundToAll(location, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.6f, 1.0f);
        playSoundToAll(location, Sound.BLOCK_LEVER_CLICK, 0.4f, 1.2f);
    }
    
    public void playDoorLock(Location location) {
        playSoundToAll(location, Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.8f);
        playSoundToAll(location, Sound.BLOCK_CHEST_LOCKED, 0.8f, 1.0f);
    }
    
    public void playDoorUnlock(Location location) {
        playSoundToAll(location, Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 1.0f);
        playSoundToAll(location, Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }
    
    public void playWinSound(Player player, boolean isMonster) {
        if (isMonster) {
            playSound(player, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.8f);
            playSound(player, Sound.ENTITY_WITHER_DEATH, 0.8f, 1.0f);
        } else {
            playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            playSound(player, Sound.BLOCK_BELL_USE, 0.8f, 1.5f);
        }
    }
    
    public void playLoseSound(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_DEATH, 1.0f, 0.8f);
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
    }
    
    public void playWhisper(Player player) {
        playSound(player, Sound.ENTITY_VILLAGER_AMBIENT, 0.3f, 1.5f);
    }
    
    public void playVoteCast(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f);
    }
    
    public void playAbilityUse(Player player) {
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.0f);
    }
    
    public void playCooldown(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
    }
    
    public void clearHeartbeatTimer(UUID playerId) {
        lastHeartbeatTime.remove(playerId);
    }
    
    // Additional sound methods for the enhanced Game class
    public void playSoundToAll(Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playSound(player, sound, volume, pitch);
        }
    }
    
    public void playGameStart() {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playGameStart(player);
        }
    }
    
    public void playMonsterAssign(Player player) {
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        playSound(player, Sound.ENTITY_GHAST_SCREAM, 0.5f, 1.5f);
    }
    
    public void playRoleAssign(Player player) {
        playSound(player, Sound.BLOCK_BELL_USE, 0.8f, 1.2f);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
    }
    
    public void playCountdown() {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }
    
    public void playActionStart() {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }
    }
    
    public void playVoteStart() {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playVoteStart(player);
        }
    }
    
    public void playAmbientHorror() {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playAmbientHorror(player.getLocation());
        }
    }
    
    public void playRoundStart() {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playSound(player, Sound.BLOCK_BELL_USE, 0.8f, 1.2f);
        }
    }
    
    public void playPlayerJoin() {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
        }
    }
    
    public void playPlayerLeave() {
        if (!plugin.getConfigManager().isSoundEnabled()) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playSound(player, Sound.ENTITY_PLAYER_DEATH, 0.4f, 1.5f);
        }
    }
    
    public void playSacrifice(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_DEATH, 1.0f, 0.7f);
        playSound(player, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
    }
}