package com.bloodties.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Logger {
    
    private static final String PREFIX = ChatColor.DARK_RED + "[BloodTies] " + ChatColor.RESET;
    
    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GREEN + message);
    }
    
    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "WARNING: " + message);
    }
    
    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.RED + "ERROR: " + message);
    }
    
    public static void debug(String message) {
        // Debug logging - can be enhanced later
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GRAY + "[DEBUG] " + message);
    }
}