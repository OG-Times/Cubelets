package me.joseph.cubelets.manager;

import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.joseph.cubelets.config.MessagesFile;
import me.joseph.cubelets.hologram.HologramService;
import me.joseph.cubelets.util.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.stream.Collectors;

public class HologramManager {

    private final Plugin plugin;
    private final HologramService holograms;
    private final MessagesFile messages;
    private final CubeletBlockManager blockManager;

    public HologramManager(Plugin plugin, HologramService holograms,
                          MessagesFile messages, CubeletBlockManager blockManager) {
        this.plugin = plugin;
        this.holograms = holograms;
        this.messages = messages;
        this.blockManager = blockManager;
    }

    public void initializeAllHolograms() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<String> cubeLocations = blockManager.getCubeletLocations();
                if (cubeLocations == null || cubeLocations.isEmpty()) {
                    return;
                }

                for (String locationStr : cubeLocations) {
                    Location loc = LocationUtils.parseLocation(locationStr);
                    if (loc == null || loc.getWorld() == null) {
                        continue;
                    }

                    Location holoLoc = LocationUtils.centerLocation(loc.clone().add(0, 2, 0));
                    String message = LocationUtils.formatText(messages.getConfig().getString("click-to-use"));
                    Hologram hologram = holograms.createTextHologram(holoLoc, message);

                    Block block = loc.getWorld().getBlockAt(holoLoc.clone().add(0, -2, 0));
                    blockManager.addBlockHologram(block, hologram);
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    public void createBlockHologram(Block block) {
        Location holoLoc = LocationUtils.centerLocation(block.getLocation()).add(0, 2, 0);
        String message = LocationUtils.formatText(messages.getConfig().getString("click-to-use"));
        Hologram hologram = holograms.createTextHologram(holoLoc, message);
        blockManager.addBlockHologram(block, hologram);
    }

    public Hologram createPlayerHologram(Block block, Player player, String rewardName, String rarity) {
        Location loc = LocationUtils.centerLocation(block.getLocation()).add(0, 2, 0);

        String line1 = LocationUtils.formatText(messages.getConfig().getString("Individual-Holo-Line-1")
                .replace("%reward-name%", rewardName));
        String line2 = LocationUtils.formatText(messages.getConfig().getString("Individual-Holo-Line-2")
                .replace("%rarity%", rarity));

        Hologram hologram = holograms.createTextHologram(loc, line1);
        hologram.getLines().appendText(line2);
        holograms.setViewers(hologram, player);

        return hologram;
    }

    public Hologram createPublicHologram(Block block, Player player, String rewardName, String rarity) {
        Location loc = LocationUtils.centerLocation(block.getLocation()).add(0, 2, 0);

        String line1 = LocationUtils.formatText(messages.getConfig().getString("Public-Holo-Line-1")
                .replace("%player%", player.getName())
                .replace("%reward-name%", rewardName));
        String line2 = LocationUtils.formatText(messages.getConfig().getString("Public-Holo-Line-2")
                .replace("%rarity%", rarity));

        Hologram hologram = holograms.createTextHologram(loc, line1);
        hologram.getLines().appendText(line2);

        List<Player> otherPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .collect(Collectors.toList());

        holograms.setViewers(hologram, otherPlayers);

        return hologram;
    }

    public void scheduleBlockReset(Block block, Hologram hologram1, Hologram hologram2, int resetDelay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                hologram1.delete();
                hologram2.delete();

                if (blockManager.isCubeletBlock(block)) {
                    createBlockHologram(block);
                }

                blockManager.removeUsedLocation(LocationUtils.centerLocation(block.getLocation()));
                blockManager.removeFireworkHeight(block);
            }
        }.runTaskLater(plugin, resetDelay * 20L);
    }

    public void cleanupAll() {
        if (holograms != null) {
            holograms.deleteAllOwnedHolograms();
        }
    }
}

