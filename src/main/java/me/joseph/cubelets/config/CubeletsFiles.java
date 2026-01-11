package me.joseph.cubelets.config;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

public class CubeletsFiles {
    private final JavaPlugin plugin;

    private final YamlFile settings;
    private final YamlFile data;
    private final YamlFile messages;

    public CubeletsFiles(JavaPlugin plugin) {
        this.plugin = plugin;

        File dataFolder = plugin.getDataFolder();
        this.settings = new YamlFile(new File(dataFolder, "settings.yml"));
        this.data = new YamlFile(new File(dataFolder, "data.yml"));
        this.messages = new YamlFile(new File(dataFolder, "messages.yml"));
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

