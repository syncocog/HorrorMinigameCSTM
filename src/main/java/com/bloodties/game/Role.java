package com.bloodties.game;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum Role {
    
    MONSTER("Monster", "🎭", ChatColor.DARK_RED, Material.SKELETON_SKULL,
            "You are the Monster, disguised as a player. Your goal is to eliminate all Survivors or escape undetected.",
            new String[]{
                "• Can disguise as any other role",
                "• Has a 'Sabotage' ability (break lights, disable doors)",
                "• Can kill at night (with cooldown)",
                "• Cannot be detected by Spectral Vision (unless cursed)"
            }),
    
    HEALER("Healer", "🩹", ChatColor.GREEN, Material.GOLDEN_APPLE,
            "You are the Healer, blessed with the power to restore life and sense deception.",
            new String[]{
                "• Can restore a player from near-death once per game",
                "• Can sense nearby injuries or lies",
                "• Immune to certain curses"
            }),
    
    WIZARD("Wizard", "🧙", ChatColor.LIGHT_PURPLE, Material.BOOK,
            "You are the Wizard, master of ancient spells and secrets.",
            new String[]{
                "• Can cast spells (detect role aura, reveal vote count)",
                "• Limited-use vision spell to see roles",
                "• Secret win path: collect all 3 soul shards"
            }),
    
    SILENT_ONE("Silent One", "😶", ChatColor.GRAY, Material.NAME_TAG,
            "You are the Silent One, bound by a curse that prevents speech.",
            new String[]{
                "• Cannot speak (via chat restriction)",
                "• Immune to votes once per game",
                "• Secret goal: survive until the end unnoticed"
            }),
    
    SURVIVOR("Survivor", "🧍", ChatColor.WHITE, Material.IRON_SWORD,
            "You are a Survivor, fighting to stay alive and identify the Monster.",
            new String[]{
                "• Base role with no special abilities",
                "• Can investigate areas",
                "• Can form blood pacts with others (vote linking)"
            });
    
    private final String displayName;
    private final String emoji;
    private final ChatColor color;
    private final Material icon;
    private final String description;
    private final String[] abilities;
    
    Role(String displayName, String emoji, ChatColor color, Material icon, String description, String[] abilities) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.color = color;
        this.icon = icon;
        this.description = description;
        this.abilities = abilities;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String[] getAbilities() {
        return abilities;
    }
    
    public String getColoredName() {
        return color + emoji + " " + displayName;
    }
    
    public boolean isMonster() {
        return this == MONSTER;
    }
    
    public boolean isSurvivor() {
        return this == SURVIVOR;
    }
    
    public boolean isSpecial() {
        return this == HEALER || this == WIZARD || this == SILENT_ONE;
    }
    
    public boolean canKill() {
        return this == MONSTER;
    }
    
    public boolean canHeal() {
        return this == HEALER;
    }
    
    public boolean canCastSpells() {
        return this == WIZARD;
    }
    
    public boolean isSilent() {
        return this == SILENT_ONE;
    }
    
    public static Role getRandomRole() {
        Role[] roles = values();
        return roles[(int) (Math.random() * roles.length)];
    }
    
    public static Role getRandomSurvivorRole() {
        Role[] survivorRoles = {HEALER, WIZARD, SILENT_ONE, SURVIVOR};
        return survivorRoles[(int) (Math.random() * survivorRoles.length)];
    }
}