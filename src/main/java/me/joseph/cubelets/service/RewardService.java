package me.joseph.cubelets.service;

import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.joseph.cubelets.config.SettingsFile;
import me.joseph.cubelets.manager.HologramManager;
import me.joseph.cubelets.util.ItemFactory;
import me.joseph.cubelets.util.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RewardService {

    private final FileConfiguration config;
    private final SettingsFile settings;
    private final ItemFactory itemFactory;
    private final HologramManager hologramManager;
    private final FireworkService fireworkService;
    private final Map<String, Integer> worldRewardSizes;

    public RewardService(FileConfiguration config, SettingsFile settings, ItemFactory itemFactory,
                        HologramManager hologramManager, FireworkService fireworkService,
                        Map<String, Integer> worldRewardSizes) {
        this.config = config;
        this.settings = settings;
        this.itemFactory = itemFactory;
        this.hologramManager = hologramManager;
        this.fireworkService = fireworkService;
        this.worldRewardSizes = worldRewardSizes;
    }

    public void giveReward(Player player, Block block) {
        String worldName = player.getWorld().getName();

        if (!worldRewardSizes.containsKey(worldName)) {
            return;
        }

        Optional<Integer> rewardId = selectRandomReward(worldName);
        if (rewardId.isEmpty()) {
            return;
        }

        int id = rewardId.get();
        processReward(player, block, worldName, id);
    }

    private Optional<Integer> selectRandomReward(String worldName) {
        int prizesSize = worldRewardSizes.get(worldName);
        int maxAttempts = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int randomId = LocationUtils.getRandomInt(0, prizesSize);
            String rewardPath = worldName + ".rewards." + randomId;

            if (!config.contains(rewardPath)) {
                continue;
            }

            int maxChance = config.getInt(worldName + ".max-chance");
            int chance = LocationUtils.getRandomInt(0, maxChance);
            int rewardChance = config.getInt(rewardPath + ".reward-chance");

            if (chance <= rewardChance) {
                return Optional.of(randomId);
            }
        }

        return Optional.empty();
    }

    private void processReward(Player player, Block block, String worldName, int rewardId) {
        String basePath = worldName + ".rewards." + rewardId;

        List<String> commands = config.getStringList(basePath + ".commands");
        executeCommands(commands, player);

        ItemStack rewardItem = createRewardItem(worldName, rewardId);

        String rewardName = config.getString(basePath + ".reward-name");
        String rarity = config.getString(basePath + ".reward-rarity");

        Hologram playerHologram = hologramManager.createPlayerHologram(block, player, rewardName, rarity);
        Hologram publicHologram = hologramManager.createPublicHologram(block, player, rewardName, rarity);

        fireworkService.launchRewardFirework(block, worldName, rewardId);

        int resetDelay = settings.getConfig().getInt("reset-after");
        hologramManager.scheduleBlockReset(block, playerHologram, publicHologram, resetDelay);

        player.updateInventory();
    }

    private void executeCommands(List<String> commands, Player player) {
        commands.stream()
                .map(cmd -> cmd.replace("%player%", player.getName()))
                .map(LocationUtils::formatText)
                .forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    private ItemStack createRewardItem(String worldName, int rewardId) {
        String basePath = worldName + ".rewards." + rewardId;
        String itemType = config.getString(basePath + ".item-type");
        return itemFactory.createItemFromConfig(itemType, worldName, rewardId, config);
    }
}

