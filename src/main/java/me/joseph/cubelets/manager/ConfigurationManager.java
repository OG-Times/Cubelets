package me.joseph.cubelets.manager;

import lombok.Getter;
import me.joseph.cubelets.config.DataFile;
import me.joseph.cubelets.config.MessagesFile;
import me.joseph.cubelets.config.SettingsFile;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ConfigurationManager {

    private final Plugin plugin;
    private DataFile data;
    private MessagesFile messages;
    private SettingsFile settings;
    private final Map<String, Integer> worldRewardSizes = new HashMap<>();

    public ConfigurationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        loadSettings();
        loadData();
        loadMessages();
        cacheWorldRewardSizes();
    }

    private void loadSettings() {
        settings = new SettingsFile(new File(plugin.getDataFolder(), "settings.yml"));
        settings.save();

        var config = settings.getConfig();
        config.addDefault("fireworks-height", 15);
        config.addDefault("fireworks-speed", 3);
        config.addDefault("reset-after", 5);
        config.addDefault("floating-item-height", 1.6D);
        config.addDefault("mysql", false);
        config.addDefault("host", "localhost");
        config.addDefault("port", "3306");
        config.addDefault("database", "testdb");
        config.addDefault("username", "root");
        config.addDefault("password", "");
        config.options().copyDefaults(true);

        settings.save();
    }

    private void loadData() {
        data = new DataFile(new File(plugin.getDataFolder(), "data.yml"));
        data.save();
        data.getConfig().options().copyDefaults(true);
        data.save();
    }

    private void loadMessages() {
        messages = new MessagesFile(new File(plugin.getDataFolder(), "messages.yml"));
        messages.save();

        var config = messages.getConfig();
        Map<String, String> defaultMessages = new HashMap<>();
        defaultMessages.put("add-cubelet-error", "&a[Cubelets] &7You already added this block as a cubelet block!");
        defaultMessages.put("add-cubelet-message", "&a[Cubelets] &eYou have added this block as a cubelet block!");
        defaultMessages.put("remove-cubelet-error", "&a[Cubelets] &7This is not a cubelet block!");
        defaultMessages.put("remove-cubelet-message", "&a[Cubelets] &eYou have removed this cubelet block!");
        defaultMessages.put("already-in-use", "&a[Cubelets] &7This cubelet block is already in use!");
        defaultMessages.put("not-enough-cubelets", "&a[Cubelets] &7You dont have any cubelets to open!");
        defaultMessages.put("not-online", "&a[Cubelets] &7That player is not online!");
        defaultMessages.put("cubelets-add", "&a[Cubelets] &eAdded &a%cubelets% &ecubelets for &a%player%&e!");
        defaultMessages.put("cubelets-remove", "&a[Cubelets] &eRemoved &a%cubelets% &ecubelets from &a%player%&e!");
        defaultMessages.put("cubelets-set", "&a[Cubelets] &eSet cubelets of &a%player% &eto &a%cubelets% &ecubelets!");
        defaultMessages.put("you-have-cubelets-message", "&a[Cubelets] &eYou have &a%amount% &ecubelets!");
        defaultMessages.put("Individual-Holo-Line-1", "&aYou found %reward-name%");
        defaultMessages.put("Individual-Holo-Line-2", "&bRarity: %rarity%");
        defaultMessages.put("Public-Holo-Line-1", "&a%player% has found %reward-name%");
        defaultMessages.put("Public-Holo-Line-2", "&bRarity: %rarity%");
        defaultMessages.put("click-to-use", "&aClick to use");

        defaultMessages.forEach(config::addDefault);
        config.options().copyDefaults(true);
        messages.save();
    }

    private void cacheWorldRewardSizes() {
        var mainConfig = plugin.getConfig();
        var configSection = mainConfig.getConfigurationSection("");
        if (configSection == null) {
            return;
        }

        for (String worldName : configSection.getKeys(false)) {
            if (worldName != null) {
                var rewardsSection = mainConfig.getConfigurationSection(worldName + ".rewards");
                if (rewardsSection != null) {
                    worldRewardSizes.put(worldName, rewardsSection.getKeys(false).size());
                }
            }
        }
    }
}

