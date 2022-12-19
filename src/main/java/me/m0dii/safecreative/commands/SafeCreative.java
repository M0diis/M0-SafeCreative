package me.m0dii.safecreative.commands;

import me.m0dii.safecreative.SafeCreativePlugin;
import me.m0dii.safecreative.listeners.PlayerListener;
import me.m0dii.safecreative.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SafeCreative implements CommandExecutor, TabCompleter {
    private final FileConfiguration cfg;
    private final SafeCreativePlugin plugin;
    public SafeCreative(SafeCreativePlugin plugin) {

        this.cfg = plugin.getCfg();
        this.plugin = plugin;
    }

    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd,
                             @Nonnull String label, @Nonnull String[] args) {
        if(isArgument(0, args, "reload")) {
            if(!sender.hasPermission("safecreative.command.reload")) {
                sender.sendMessage(Utils.format(cfg.getString("messages.no-permission")));

                return true;
            }

            plugin.getConfigManager().reloadConfig();

            sender.sendMessage(Utils.format(cfg.getString("messages.reloaded")));

            return true;
        }

        if(isArgument(0, args, "toggle")) {
            if(!sender.hasPermission("safecreative.command.toggle")) {
                sender.sendMessage(Utils.format(cfg.getString("messages.no-permission")));

                return true;
            }

            PlayerListener.toggleEnabled();

            if(PlayerListener.isEnabled()) {
                sender.sendMessage(Utils.format(cfg.getString("messages.protection-enabled")));
            } else {
                sender.sendMessage(Utils.format(cfg.getString("messages.protection-disabled")));
            }

            return true;
        }

        if(isArgument(0, args, "on")) {
            if(!sender.hasPermission("safecreative.command.on")) {
                sender.sendMessage(Utils.format(cfg.getString("messages.no-permission")));

                return true;
            }

            PlayerListener.setEnabled(true);

            sender.sendMessage(Utils.format(cfg.getString("messages.protection-enabled")));

            return true;
        }

        if(isArgument(0, args, "off")) {
            if(!sender.hasPermission("safecreative.command.off")) {
                sender.sendMessage(Utils.format(cfg.getString("messages.no-permission")));

                return true;
            }

            PlayerListener.setEnabled(false);

            sender.sendMessage(Utils.format(cfg.getString("messages.protection-disabled")));

            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command cmd,
                                      @Nonnull String alias, @Nonnull String[] args) {
        List<String> completes = new ArrayList<>();

        if(args.length == 1) {
            Stream.of("reload")
                    .filter(s -> StringUtils.startsWithIgnoreCase(s, args[0]))
                    .forEach(completes::add);
        }

        return completes;
    }


    private boolean isArgument(int index, String[] args, String... argument) {
        if(args.length == 0 || args.length < index) {
            return false;
        }

        return Arrays.stream(argument).anyMatch(arg -> args[index].equalsIgnoreCase(arg));
    }
}