package me.joseph.cubelets.listener;

import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.joseph.cubelets.CubeletsAPI;
import me.joseph.cubelets.config.MessagesFile;
import me.joseph.cubelets.manager.CubeletBlockManager;
import me.joseph.cubelets.service.FireworkService;
import me.joseph.cubelets.service.RewardService;
import me.joseph.cubelets.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class CubeletEventListener implements Listener {

    private final Plugin plugin;
    private final CubeletsAPI api;
    private final MessagesFile messages;
    private final CubeletBlockManager blockManager;
    private final FireworkService fireworkService;
    private final RewardService rewardService;
    private final Set<String> playerCooldowns = new HashSet<>();

    private static final long COOLDOWN_TICKS = 20L;

    public CubeletEventListener(Plugin plugin, CubeletsAPI api, MessagesFile messages,
                               CubeletBlockManager blockManager, FireworkService fireworkService,
                               RewardService rewardService) {
        this.plugin = plugin;
        this.api = api;
        this.messages = messages;
        this.blockManager = blockManager;
        this.fireworkService = fireworkService;
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!api.existsInDatabase(player)) {
            api.createCubelets(player);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!blockManager.isCubeletBlock(block)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isOp() && !player.hasPermission("cubelets.admin")) {
            event.setCancelled(true);
            return;
        }

        player.sendMessage(LocationUtils.formatText(messages.getConfig().getString("remove-cubelet-message")));
        blockManager.removeCubeletBlock(block);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !blockManager.isCubeletBlock(clickedBlock)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!handleCooldown(player)) {
            return;
        }

        Location fixedLocation = LocationUtils.centerLocation(clickedBlock.getLocation());

        if (blockManager.isLocationInUse(fixedLocation)) {
            player.sendMessage(LocationUtils.formatText(messages.getConfig().getString("already-in-use")));
            return;
        }

        if (api.getCubelets(player) < 1) {
            player.sendMessage(LocationUtils.formatText(messages.getConfig().getString("not-enough-cubelets")));
            return;
        }

        startCubeletAnimation(player, clickedBlock, fixedLocation);
    }

    private boolean handleCooldown(Player player) {
        String playerName = player.getName();

        if (playerCooldowns.contains(playerName)) {
            return false;
        }

        playerCooldowns.add(playerName);

        new BukkitRunnable() {
            @Override
            public void run() {
                playerCooldowns.remove(playerName);
            }
        }.runTaskLater(plugin, COOLDOWN_TICKS);

        return true;
    }

    private void startCubeletAnimation(Player player, Block block, Location fixedLocation) {
        api.removeCubelets(player, 1);
        blockManager.addUsedLocation(fixedLocation);

        Hologram existingHologram = blockManager.removeBlockHologram(block);
        if (existingHologram != null) {
            existingHologram.delete();
        }

        fireworkService.startFireworkAnimation(player, block, fixedLocation,
                () -> rewardService.giveReward(player, block));
    }
}

