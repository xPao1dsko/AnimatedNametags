package me.animatednametags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NametagCommand implements CommandExecutor, TabCompleter {

    private final AnimatedNametagsPlugin plugin;
    private final NametagManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public NametagCommand(AnimatedNametagsPlugin plugin, NametagManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "usage");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("animatednametags.admin")) {
                    send(sender, "no-permission");
                    return true;
                }
                manager.reload();
                send(sender, "reloaded");
            }
            case "refresh" -> {
                if (!sender.hasPermission("animatednametags.admin")) {
                    send(sender, "no-permission");
                    return true;
                }
                manager.refreshAll();
                send(sender, "refreshed");
            }
            case "toggle" -> {
                Player target;
                if (args.length >= 2) {
                    if (!sender.hasPermission("animatednametags.admin")) {
                        send(sender, "no-permission");
                        return true;
                    }
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        send(sender, "player-not-found");
                        return true;
                    }
                } else if (sender instanceof Player self) {
                    if (!sender.hasPermission("animatednametags.toggle")) {
                        send(sender, "no-permission");
                        return true;
                    }
                    target = self;
                } else {
                    send(sender, "usage");
                    return true;
                }
                boolean enabled = manager.toggle(target);
                sendWithTarget(sender, enabled ? "toggled-on" : "toggled-off", target.getName());
            }
            default -> send(sender, "usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("toggle", "reload", "refresh")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")
                && sender.hasPermission("animatednametags.admin")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }

    private void send(CommandSender sender, String key) {
        sendWithTarget(sender, key, "");
    }

    private void sendWithTarget(CommandSender sender, String key, String target) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String msg = plugin.getConfig().getString("messages." + key, key);
        Component component = mm.deserialize(prefix + msg, Placeholder.unparsed("target", target));
        sender.sendMessage(component);
    }
}
