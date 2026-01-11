package me.joseph.cubelets;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class settings {
    private FileConfiguration config;
    private File file;

    public settings(File paramFile) {
        this.file = paramFile;
        this.config = YamlConfiguration.loadConfiguration(paramFile);
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (IOException var2) {
            var2.printStackTrace();
        }

    }
}
