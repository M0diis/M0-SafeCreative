package me.m0dii.safecreative;

import me.m0dii.pllib.UpdateChecker;
import me.m0dii.pllib.data.ConfigManager;
import me.m0dii.safecreative.listeners.PlayerListener;
import me.m0dii.safecreative.utils.Utils;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SafeCreative extends JavaPlugin {
    private static SafeCreative instance;
    private PluginManager pm;
    private ConfigManager configManager;

    public static SafeCreative getInstance() {
        return instance;
    }

    public FileConfiguration getCfg() {
        return this.configManager.getConfig();
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);

        this.pm = this.getServer().getPluginManager();

        Utils.copy(getResource("config.yml"), new File(getDataFolder(), "config_default.yml"));

        registerCommands();
//        checkForUpdates();

        getLogger().info("SafeCreative has been enabled.");
    }

    private void checkForUpdates() {
        new UpdateChecker(this, 106730).getVersion(ver ->
        {
            String curr = this.getDescription().getVersion();

            if (!curr.equalsIgnoreCase(ver.replace("v", ""))) {
                getLogger().info("You are running an outdated version of SafeCreative.");
                getLogger().info("Latest version: " + ver + ", you are using: " + curr);
//                getLogger().info("You can download the latest version on Spigot:");
//                getLogger().info("https://www.spigotmc.org/resources/106730/");
            }
        });
    }

    public void onDisable() {
        this.getLogger().info("SafeCreative has been disabled.");
    }

    private void registerCommands() {
        PluginCommand cmd = getCommand("safecreative");

        if (cmd != null) {
            cmd.setExecutor(new me.m0dii.safecreative.commands.SafeCreative(this));
        }

        pm.registerEvents(new PlayerListener(this), this);
    }
}