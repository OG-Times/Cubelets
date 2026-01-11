package me.joseph.cubelets.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.UUID;

public class ItemFactory {

    private final Plugin plugin;

    public ItemFactory(Plugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createPlayerHead(String playerName) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(playerName);
        skull.setItemMeta(meta);
        return skull;
    }

    public ItemStack createCustomHead(String textureId) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", textureId));

        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().warning("Error al crear custom head: " + e.getMessage());
        }

        head.setItemMeta(meta);
        return head;
    }

    public ItemStack createItemFromConfig(String itemType, String worldName, int rewardId,
                                          org.bukkit.configuration.file.FileConfiguration config) {
        if (itemType == null) {
            return null;
        }

        String basePath = worldName + ".rewards." + rewardId;

        switch (itemType.toLowerCase()) {
            case "head":
                return createPlayerHead(config.getString(basePath + ".head-name"));
            case "item":
                int itemId = config.getInt(basePath + ".reward-item-id");
                int damage = config.getInt(basePath + ".reward-item-damage");
                return new ItemStack(Material.getMaterial(itemId), 1, (short) damage);
            case "customhead":
                return createCustomHead(config.getString(basePath + ".custom-head-id"));
            default:
                return null;
        }
    }
}

