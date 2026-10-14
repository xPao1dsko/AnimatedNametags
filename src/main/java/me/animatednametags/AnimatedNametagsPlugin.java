package me.animatednametags;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AnimatedNametagsPlugin extends JavaPlugin {

    private NametagManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        manager = new NametagManager(this);
        manager.start();

        getServer().getPluginManager().registerEvents(new PlayerListener(manager), this);

        NametagCommand command = new NametagCommand(this, manager);
        PluginCommand pluginCommand = getCommand("animatednametags");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
    }
}
