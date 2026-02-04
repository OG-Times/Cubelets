package me.joseph.cubelets.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class CubeletsFiles {
    private final JavaPlugin plugin;

    private final YamlFile settings;
    private final YamlFile data;
    private final YamlFile messages;

    public CubeletsFiles(JavaPlugin plugin) {
        this.plugin = plugin;

        File dataFolder = plugin.getDataFolder();
        this.settings = new YamlFile(new File(dataFolder, "SettingsConfig.yml"));
        this.data = new YamlFile(new File(dataFolder, "DataConfig.yml"));
        this.messages = new YamlFile(new File(dataFolder, "MessagesConfig.yml"));
    }

    public YamlFile settings() {
        return settings;
    }

    public YamlFile data() {
        return data;
    }

    public YamlFile messages() {
        return messages;
    }

    public void saveAll() {
        settings.save();
        data.save();
        messages.save();
    }

    public void reloadAll() {
        settings.reload();
        data.reload();
        messages.reload();
    }
}

