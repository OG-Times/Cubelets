package me.joseph.cubelets.service;

import me.joseph.cubelets.config.SettingsFile;
import me.joseph.cubelets.manager.CubeletBlockManager;
import me.joseph.cubelets.util.LocationUtils;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class FireworkService {

    private final Plugin plugin;
    private final FileConfiguration config;
    private final SettingsFile settings;
    private final CubeletBlockManager blockManager;

    public FireworkService(Plugin plugin, FileConfiguration config,
                          SettingsFile settings, CubeletBlockManager blockManager) {
        this.plugin = plugin;
        this.config = config;
        this.settings = settings;
        this.blockManager = blockManager;
    }

    public void launchFirework(Location location) {
        String worldName = location.getWorld().getName();
        List<Color> colors = LocationUtils.parseColors(config.getStringList(worldName + ".firework.colors"));
        List<Color> fade = LocationUtils.parseColors(config.getStringList(worldName + ".firework.fade"));

        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Builder effectBuilder = FireworkEffect.builder()
                .flicker(config.getBoolean(worldName + ".firework.flicker"))
                .trail(config.getBoolean(worldName + ".firework.trail"))
                .with(FireworkEffect.Type.valueOf(config.getString(worldName + ".firework.type")))
                .withColor(colors)
                .withFade(fade);

        meta.addEffect(effectBuilder.build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);

        new BukkitRunnable() {
            @Override
            public void run() {
                firework.detonate();
            }
        }.runTaskLater(plugin, 1L);
    }

    public void launchRewardFirework(Block block, String worldName, int rewardId) {
        String basePath = worldName + ".rewards." + rewardId;
        List<Color> colors = LocationUtils.parseColors(config.getStringList(basePath + ".firework.colors"));
        List<Color> fade = LocationUtils.parseColors(config.getStringList(basePath + ".firework.fade"));

        Location location = LocationUtils.centerLocation(block.getLocation());
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Builder effectBuilder = FireworkEffect.builder()
                .flicker(config.getBoolean(basePath + ".firework.flicker"))
                .trail(config.getBoolean(basePath + ".firework.trail"))
                .with(FireworkEffect.Type.valueOf(config.getString(basePath + ".firework.type")))
                .withColor(colors)
                .withFade(fade);

        meta.addEffect(effectBuilder.build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);

        new BukkitRunnable() {
            @Override
            public void run() {
                firework.detonate();
            }
        }.runTaskLater(plugin, 1L);
    }

    public void startFireworkAnimation(Player player, Block block, Location fixedLocation, Runnable onComplete) {
        int height = settings.getConfig().getInt("fireworks-height");
        blockManager.setFireworkHeight(block, height);

        final double baseY = fixedLocation.getY();
        final int speed = settings.getConfig().getInt("fireworks-speed");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                int remaining = blockManager.getFireworkHeight(block);
                Location fireworkLoc = LocationUtils.centerLocation(block.getLocation()).add(0, remaining, 0);
                blockManager.setFireworkHeight(block, remaining - 1);

                if (baseY < fireworkLoc.getY()) {
                    launchFirework(fireworkLoc);
                }

                if (fireworkLoc.getY() == baseY) {
                    onComplete.run();
                }

                if (fireworkLoc.getY() <= baseY) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, speed, speed);
    }
}

