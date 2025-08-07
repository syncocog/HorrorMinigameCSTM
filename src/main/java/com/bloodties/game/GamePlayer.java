package com.bloodties.game;

import com.bloodties.BloodTiesPlugin;
import com.bloodties.managers.DataManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GamePlayer {
    
    private final UUID playerId;
    private final String name;
    private Role role;
    private boolean isAlive;
    private boolean isDisguised;
    private Role disguisedAs;
    private int kills;
    private int votes;
    private int correctVotes;
    private long lastAbilityUse;
    private long lastKillTime;
    private boolean hasUsedHeal;
    private boolean hasUsedVoteImmunity;
    private int soulShards;
    private Map<String, Long> cooldowns;
    private GamePlayer bloodPactPartner;
    
    public GamePlayer(Player player) {
        this.playerId = player.getUniqueId();
        this.name = player.getName();
        this.role = Role.SURVIVOR;
        this.isAlive = true;
        this.isDisguised = false;
        this.disguisedAs = null;
        this.kills = 0;
        this.votes = 0;
        this.correctVotes = 0;
        this.lastAbilityUse = 0;
        this.lastKillTime = 0;
        this.hasUsedHeal = false;
        this.hasUsedVoteImmunity = false;
        this.soulShards = 0;
        this.cooldowns = new HashMap<>();
        this.bloodPactPartner = null;
    }
    
    // Getters and Setters
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getName() {
        return name;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public boolean isAlive() {
        return isAlive;
    }
    
    public void setAlive(boolean alive) {
        isAlive = alive;
    }
    
    public boolean isDisguised() {
        return isDisguised;
    }
    
    public void setDisguised(boolean disguised) {
        isDisguised = disguised;
    }
    
    public Role getDisguisedAs() {
        return disguisedAs;
    }
    
    public void setDisguisedAs(Role disguisedAs) {
        this.disguisedAs = disguisedAs;
    }
    
    public int getKills() {
        return kills;
    }
    
    public void incrementKills() {
        this.kills++;
    }
    
    public int getVotes() {
        return votes;
    }
    
    public void incrementVotes() {
        this.votes++;
    }
    
    public int getCorrectVotes() {
        return correctVotes;
    }
    
    public void incrementCorrectVotes() {
        this.correctVotes++;
    }
    
    public long getLastAbilityUse() {
        return lastAbilityUse;
    }
    
    public void setLastAbilityUse(long lastAbilityUse) {
        this.lastAbilityUse = lastAbilityUse;
    }
    
    public long getLastKillTime() {
        return lastKillTime;
    }
    
    public void setLastKillTime(long lastKillTime) {
        this.lastKillTime = lastKillTime;
    }
    
    public boolean hasUsedHeal() {
        return hasUsedHeal;
    }
    
    public void setHasUsedHeal(boolean hasUsedHeal) {
        this.hasUsedHeal = hasUsedHeal;
    }
    
    public boolean hasUsedVoteImmunity() {
        return hasUsedVoteImmunity;
    }
    
    public void setHasUsedVoteImmunity(boolean hasUsedVoteImmunity) {
        this.hasUsedVoteImmunity = hasUsedVoteImmunity;
    }
    
    public int getSoulShards() {
        return soulShards;
    }
    
    public void addSoulShard() {
        this.soulShards++;
    }
    
    public GamePlayer getBloodPactPartner() {
        return bloodPactPartner;
    }
    
    public void setBloodPactPartner(GamePlayer bloodPactPartner) {
        this.bloodPactPartner = bloodPactPartner;
    }
    
    // Utility methods
    public boolean isMonster() {
        return role == Role.MONSTER;
    }
    
    public boolean isSurvivor() {
        return role == Role.SURVIVOR;
    }
    
    public boolean isHealer() {
        return role == Role.HEALER;
    }
    
    public boolean isWizard() {
        return role == Role.WIZARD;
    }
    
    public boolean isSilentOne() {
        return role == Role.SILENT_ONE;
    }
    
    public Role getDisplayRole() {
        return isDisguised ? disguisedAs : role;
    }
    
    public String getDisplayRoleName() {
        return getDisplayRole().getColoredName();
    }
    
    public boolean canUseAbility() {
        long currentTime = System.currentTimeMillis();
        long cooldown = getAbilityCooldown();
        return currentTime - lastAbilityUse >= cooldown;
    }
    
    public boolean canKill() {
        if (!isMonster()) return false;
        long currentTime = System.currentTimeMillis();
        long cooldown = BloodTiesPlugin.getInstance().getConfigManager().getMonsterKillCooldown() * 1000L;
        return currentTime - lastKillTime >= cooldown;
    }
    
    public long getAbilityCooldown() {
        switch (role) {
            case MONSTER:
                return BloodTiesPlugin.getInstance().getConfigManager().getMonsterKillCooldown() * 1000L;
            case HEALER:
                return BloodTiesPlugin.getInstance().getConfigManager().getHealerHealCooldown() * 1000L;
            case WIZARD:
                return BloodTiesPlugin.getInstance().getConfigManager().getWizardSpellCooldown() * 1000L;
            default:
                return 0;
        }
    }
    
    public long getRemainingCooldown() {
        long currentTime = System.currentTimeMillis();
        long cooldown = getAbilityCooldown();
        long timeSinceLastUse = currentTime - lastAbilityUse;
        return Math.max(0, cooldown - timeSinceLastUse);
    }
    
    public void setCooldown(String ability, long duration) {
        cooldowns.put(ability, System.currentTimeMillis() + duration);
    }
    
    public boolean isOnCooldown(String ability) {
        Long endTime = cooldowns.get(ability);
        return endTime != null && System.currentTimeMillis() < endTime;
    }
    
    public long getRemainingCooldown(String ability) {
        Long endTime = cooldowns.get(ability);
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    public void clearCooldown(String ability) {
        cooldowns.remove(ability);
    }
    
    public void resetCooldowns() {
        cooldowns.clear();
        lastAbilityUse = 0;
        lastKillTime = 0;
    }
    
    public void disguiseAs(Role role) {
        this.isDisguised = true;
        this.disguisedAs = role;
    }
    
    public void removeDisguise() {
        this.isDisguised = false;
        this.disguisedAs = null;
    }
    
    public boolean hasWonSecretPath() {
        if (isWizard() && soulShards >= 3) return true;
        if (isSilentOne() && isAlive) return true; // Silent One wins if they survive
        return false;
    }
    
    public void saveStats() {
        DataManager dataManager = BloodTiesPlugin.getInstance().getDataManager();
        dataManager.getPlayerData(playerId).setTotalKills(dataManager.getPlayerData(playerId).getTotalKills() + kills);
        dataManager.getPlayerData(playerId).setTotalVotes(dataManager.getPlayerData(playerId).getTotalVotes() + votes);
        dataManager.getPlayerData(playerId).setCorrectVotes(dataManager.getPlayerData(playerId).getCorrectVotes() + correctVotes);
    }
    
    public String getStatusMessage() {
        if (!isAlive) {
            return ChatColor.RED + "☠ DEAD";
        }
        
        StringBuilder status = new StringBuilder();
        status.append(ChatColor.GREEN).append("❤ ALIVE");
        
        if (isDisguised) {
            status.append(ChatColor.YELLOW).append(" (Disguised as ").append(disguisedAs.getDisplayName()).append(")");
        }
        
        if (bloodPactPartner != null) {
            status.append(ChatColor.LIGHT_PURPLE).append(" [Blood Pact: ").append(bloodPactPartner.getName()).append("]");
        }
        
        return status.toString();
    }
}