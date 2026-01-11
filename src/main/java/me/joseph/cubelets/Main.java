package me.joseph.cubelets;

import lombok.Getter;
import me.joseph.cubelets.hologram.HologramService;
import me.joseph.cubelets.listener.CubeletEventListener;
import me.joseph.cubelets.manager.ConfigurationManager;
import me.joseph.cubelets.manager.CubeletBlockManager;
import me.joseph.cubelets.manager.HologramManager;
import me.joseph.cubelets.service.FireworkService;
import me.joseph.cubelets.service.RewardService;
import me.joseph.cubelets.sql.SQLConnection;
import me.joseph.cubelets.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Main extends JavaPlugin {

    @Getter
    private static Main instance;

    private CubeletsAPI api;
    private SQLConnection sqlConnection;

    // Managers
    private ConfigurationManager configManager;
    private CubeletBlockManager blockManager;
    private HologramManager hologramManager;

    // Services
    private HologramService hologramService;
    private FireworkService fireworkService;
    private RewardService rewardService;
    private ItemFactory itemFactory;


    @Override
    public void onEnable() {
        instance = this;

        initializeServices();
        loadConfigurations();
        registerEvents();
        connectDatabase();
        registerOnlinePlayers();

        getLogger().info("Cubelets plugin habilitado exitosamente!");
    }

    @Override
    public void onDisable() {
        cleanupHolograms();
        closeDatabaseConnection();
        getLogger().info("Cubelets plugin deshabilitado.");
    }

    // ==================== INICIALIZACIÓN ====================

    private void initializeServices() {
        this.api = new CubeletsAPI();
        this.hologramService = new HologramService(this);
        this.itemFactory = new ItemFactory(this);
    }

    private void loadConfigurations() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.configManager = new ConfigurationManager(this);
        configManager.loadAll();

        this.blockManager = new CubeletBlockManager(configManager.getData());
        this.hologramManager = new HologramManager(this, hologramService,
                configManager.getMessages(), blockManager);
        this.fireworkService = new FireworkService(this, getConfig(),
                configManager.getSettings(), blockManager);
        this.rewardService = new RewardService(getConfig(), configManager.getSettings(),
                itemFactory, hologramManager, fireworkService, configManager.getWorldRewardSizes());

        hologramManager.initializeAllHolograms();
    }

    private void registerEvents() {
        CubeletEventListener listener = new CubeletEventListener(this, api,
                configManager.getMessages(), blockManager, fireworkService, rewardService);
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void connectDatabase() {
        if (!configManager.getSettings().getConfig().getBoolean("mysql")) {
            return;
        }

        var settings = configManager.getSettings().getConfig();
        String host = settings.getString("host");
        String port = settings.getString("port");
        String database = settings.getString("database");
        String username = settings.getString("username");
        String password = settings.getString("password");

        sqlConnection = new SQLConnection(this, host, port, database, username, password);
        sqlConnection.openConnection();
    }

    private void registerOnlinePlayers() {
        if (configManager.getSettings().getConfig().getBoolean("mysql")) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!api.existsInDatabase(player)) {
                    api.createCubelets(player);
                }
            });
        }
    }

    private void cleanupHolograms() {
        if (hologramManager != null) {
            hologramManager.cleanupAll();
        }
    }

    private void closeDatabaseConnection() {
        if (sqlConnection != null) {
            sqlConnection.closeConnection();
        }
    }


    // ==================== BLOCK MANAGEMENT (API Pública) ====================

    public void addCubeletBlock(Block block) {
        if (blockManager.isCubeletBlock(block)) {
            return;
        }
        blockManager.addCubeletBlock(block);
        hologramManager.createBlockHologram(block);
    }

    public void removeCubeletBlock(Block block) {
        blockManager.removeCubeletBlock(block);
    }

    public boolean isCubeletBlock(Block block) {
        return blockManager.isCubeletBlock(block);
    }
}
