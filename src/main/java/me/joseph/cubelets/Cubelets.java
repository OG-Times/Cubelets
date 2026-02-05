package me.joseph.cubelets;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.Getter;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.joseph.cubelets.api.CubeletsAPI;
import me.joseph.cubelets.command.CubeletsCommand;
import me.joseph.cubelets.config.DataConfig;
import me.joseph.cubelets.config.MessagesConfig;
import me.joseph.cubelets.config.SettingsConfig;
import me.joseph.cubelets.hologram.HologramService;
import me.joseph.cubelets.sql.SQLConnection;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Getter
public class Cubelets extends JavaPlugin implements Listener {
    @Getter public static Cubelets instance;

    HashMap<String, Integer> size = new HashMap<>();
    public SQLConnection sqlConnection = null;
    public DataConfig dataConfig;
    public MessagesConfig messagesConfig;
    public SettingsConfig settingsConfig;
    ArrayList<String> cooldown = new ArrayList<>();
    public ArrayList<Location> used = new ArrayList<>();
    public HashMap<Block, Hologram> holo = new HashMap<>();
    public HashMap<Block, Integer> height = new HashMap<>();
    private HologramService hologramService;
    private final Map<String, Long> logThrottle = new HashMap<>();
    private final Map<UUID, Integer> cubeletCache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        this.hologramService = new HologramService(this);
        this.settingsConfig = new SettingsConfig(new File(this.getDataFolder() + "/settings.yml"));
        this.settingsConfig.save();
        this.settingsConfig.getConfig().addDefault("fireworks-height", 15);
        this.settingsConfig.getConfig().addDefault("fireworks-speed", 3);
        this.settingsConfig.getConfig().addDefault("reset-after", 5);
        this.settingsConfig.getConfig().addDefault("floating-item-height", 1.6D);
        this.settingsConfig.getConfig().addDefault("mysql", false);
        this.settingsConfig.getConfig().addDefault("host", "localhost");
        this.settingsConfig.getConfig().addDefault("port", "3306");
        this.settingsConfig.getConfig().addDefault("database", "testdb");
        this.settingsConfig.getConfig().addDefault("username", "root");
        this.settingsConfig.getConfig().addDefault("password", "");
        this.settingsConfig.getConfig().options().copyDefaults(true);
        this.settingsConfig.save();
        this.dataConfig = new DataConfig(new File(this.getDataFolder() + "/data.yml"));
        this.dataConfig.save();
        this.dataConfig.getConfig().options().copyDefaults(true);
        this.dataConfig.save();
        this.messagesConfig = new MessagesConfig(new File(this.getDataFolder() + "/messages.yml"));
        this.messagesConfig.save();
        this.messagesConfig.getConfig().addDefault("add-cubelet-error", "&a[Cubelets] &7You already added this block as a cubelet block!");
        this.messagesConfig.getConfig().addDefault("add-cubelet-message", "&a[Cubelets] &eYou have added this block as a cubelet block!");
        this.messagesConfig.getConfig().addDefault("remove-cubelet-error", "&a[Cubelets] &7This is not a cubelet block!");
        this.messagesConfig.getConfig().addDefault("remove-cubelet-message", "&a[Cubelets] &eYou have removed this cubelet block!");
        this.messagesConfig.getConfig().addDefault("already-in-use", "&a[Cubelets] &7This cubelet block is already in use!");
        this.messagesConfig.getConfig().addDefault("not-enough-cubelets", "&a[Cubelets] &7You dont have any cubelets to open!");
        this.messagesConfig.getConfig().addDefault("not-online", "&a[Cubelets] &7That player is not online!");
        this.messagesConfig.getConfig().addDefault("cubelets-add", "&a[Cubelets] &eAdded &a%cubelets% &ecubelets for &a%player%&e!");
        this.messagesConfig.getConfig().addDefault("cubelets-remove", "&a[Cubelets] &eRemoved &a%cubelets% &ecubelets from &a%player%&e!");
        this.messagesConfig.getConfig().addDefault("cubelets-set", "&a[Cubelets] &eSet cubelets of &a%player% &eto &a%cubelets% &ecubelets!");
        this.messagesConfig.getConfig().addDefault("you-have-cubelets-message", "&a[Cubelets] &eYou have &a%amount% &ecubelets!");
        this.messagesConfig.getConfig().addDefault("Individual-Holo-Line-1", "&aYou found %reward-name%");
        this.messagesConfig.getConfig().addDefault("Individual-Holo-Line-2", "&bRarity: %rarity%");
        this.messagesConfig.getConfig().addDefault("Public-Holo-Line-1", "&a%player% has found %reward-name%");
        this.messagesConfig.getConfig().addDefault("Public-Holo-Line-2", "&bRarity: %rarity%");
        this.messagesConfig.getConfig().addDefault("click-to-use", "&aClick to use");
        this.messagesConfig.getConfig().options().copyDefaults(true);
        this.messagesConfig.save();
        instance = this;

        Bukkit.getCommandMap().register("cubelets", new CubeletsCommand(this));

        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        (new BukkitRunnable() {
            public void run() {
                List<String> c = Cubelets.this.dataConfig.getConfig().getStringList("cube-list");
                if (c != null && !c.isEmpty()) {
                    for (String s : c) {
                        Location loc = Cubelets.getLocationFromString(s);
                        if (loc == null || loc.getWorld() == null) {
                            continue;
                        }
                        Location holoLoc = Cubelets.this.getFixedLocation(loc.clone().add(0.0D, 2.0D, 0.0D));
                        Hologram h = Cubelets.this.hologramService.createTextHologram(holoLoc, Cubelets.this.formatText(Cubelets.this.messagesConfig.getConfig().getString("click-to-use")));
                        Cubelets.this.holo.put(loc.getWorld().getBlockAt(holoLoc.clone().add(0.0D, -2.0D, 0.0D)), h);
                    }
                }
            }
        }).runTaskLater(this, 100L);

        if (this.settingsConfig.getConfig().getBoolean("mysql")) {
            String host = this.settingsConfig.getConfig().getString("host");
            String port = this.settingsConfig.getConfig().getString("port");
            String database = this.settingsConfig.getConfig().getString("database");
            String username = this.settingsConfig.getConfig().getString("username");
            String password = this.settingsConfig.getConfig().getString("password");
            this.sqlConnection = new SQLConnection(this, host, port, database, username, password);
            this.sqlConnection.openConnection();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.register(player);
        }

        for (String s : this.getConfig().getConfigurationSection("").getKeys(false)) {
            if (s != null) {
                this.size.put(s, this.getConfig().getConfigurationSection(s + ".rewards").getKeys(false).size());
            }
        }

    }

    @Override
    public void onDisable() {
        if (this.hologramService != null) {
            this.hologramService.deleteAllOwnedHolograms();
        }
    }

    public Color getColor(String paramString) {
        if (paramString.equalsIgnoreCase("AQUA")) {
            return Color.AQUA;
        } else if (paramString.equalsIgnoreCase("BLACK")) {
            return Color.BLACK;
        } else if (paramString.equalsIgnoreCase("BLUE")) {
            return Color.BLUE;
        } else if (paramString.equalsIgnoreCase("FUCHSIA")) {
            return Color.FUCHSIA;
        } else if (paramString.equalsIgnoreCase("GRAY")) {
            return Color.GRAY;
        } else if (paramString.equalsIgnoreCase("GREEN")) {
            return Color.GREEN;
        } else if (paramString.equalsIgnoreCase("LIME")) {
            return Color.LIME;
        } else if (paramString.equalsIgnoreCase("MAROON")) {
            return Color.MAROON;
        } else if (paramString.equalsIgnoreCase("NAVY")) {
            return Color.NAVY;
        } else if (paramString.equalsIgnoreCase("OLIVE")) {
            return Color.OLIVE;
        } else if (paramString.equalsIgnoreCase("ORANGE")) {
            return Color.ORANGE;
        } else if (paramString.equalsIgnoreCase("PURPLE")) {
            return Color.PURPLE;
        } else if (paramString.equalsIgnoreCase("RED")) {
            return Color.RED;
        } else if (paramString.equalsIgnoreCase("SILVER")) {
            return Color.SILVER;
        } else if (paramString.equalsIgnoreCase("TEAL")) {
            return Color.TEAL;
        } else if (paramString.equalsIgnoreCase("WHITE")) {
            return Color.WHITE;
        } else {
            return paramString.equalsIgnoreCase("YELLOW") ? Color.YELLOW : null;
        }
    }

    public void LaunchFirework(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            logThrottled("firework.null-location", Level.WARNING, "[Cubelets] Firework skipped: location/world is null.", 30000L);
            return;
        }
        ArrayList<Color> colors = new ArrayList<>();
        ArrayList<Color> fade = new ArrayList<>();
        List<String> lore = this.getConfig().getStringList(loc.getWorld().getName() + ".firework.colors");
        List<String> lore2 = this.getConfig().getStringList(loc.getWorld().getName() + ".firework.fade");

        for (String string : lore) {
            Color color = this.getColor(string);
            if (color != null) {
                colors.add(color);
            }
        }

        for (String string : lore2) {
            Color color = this.getColor(string);
            if (color != null) {
                fade.add(color);
            }
        }

        String typeName = this.getConfig().getString(loc.getWorld().getName() + ".firework.type");
        if (typeName == null || typeName.trim().isEmpty()) {
            logThrottled("firework.missing-type." + loc.getWorld().getName(), Level.WARNING,
                    "[Cubelets] Firework skipped: missing firework.type for world '" + loc.getWorld().getName() + "'.", 30000L);
            return;
        }

        Type fireworkType;
        try {
            fireworkType = Type.valueOf(typeName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logThrottled("firework.bad-type." + loc.getWorld().getName(), Level.WARNING,
                    "[Cubelets] Firework skipped: invalid firework.type '" + typeName + "' for world '" + loc.getWorld().getName() + "'.", 30000L);
            return;
        }

        final Firework firework = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(
                FireworkEffect
                        .builder()
                        .flicker(this.getConfig().getBoolean(loc.getWorld().getName() + ".firework.flicker"))
                        .trail(this.getConfig().getBoolean(loc.getWorld().getName() + ".firework.trail"))
                        .with(fireworkType)
                        .withColor(colors)
                        .withFade(fade)
                        .build()
        );
        meta.setPower(1);
        firework.setFireworkMeta(meta);

        new BukkitRunnable() {
            public void run() {
                firework.detonate();
            }
        }.runTaskLater(getInstance(), 1L);
    }

    public void register(Player player) {
        if (this.settingsConfig.getConfig().getBoolean("mysql")) {
            CubeletsAPI.existsInDatabaseAsync(player).thenAccept(exists -> {
                if (!exists) {
                    this.cubeletCache.put(player.getUniqueId(), 0);
                    CubeletsAPI.createCubeletsAsync(player);
                    return;
                }
                CubeletsAPI.getCubeletsAsync(player).thenAccept(amount -> this.cubeletCache.put(player.getUniqueId(), amount));
            });
        }

    }

    @EventHandler
    public void onLogin(PlayerJoinEvent e) {
        this.register(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!this.settingsConfig.getConfig().getBoolean("mysql")) {
            return;
        }
        Player player = e.getPlayer();
        Integer cached = this.cubeletCache.remove(player.getUniqueId());
        if (cached != null) {
            CubeletsAPI.flushCubeletsAsync(player, cached);
        }
    }

    public Location getFixedLocation(Location loc) {
        return loc.add(0.5D, 0.0D, 0.5D);
    }

    @EventHandler
    public void onInteract(BlockBreakEvent e) {
        if (this.hasPotion(e.getBlock())) {
            if (!e.getPlayer().isOp() && !e.getPlayer().hasPermission("cubelets.admin")) {
                e.setCancelled(true);
                return;
            }

            e.getPlayer().sendMessage(this.formatText(this.messagesConfig.getConfig().getString("remove-cubelet-message")));
            this.removePotion(e.getBlock());
        }

    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && this.hasPotion(e.getClickedBlock())) {
            e.setCancelled(true);
            if (this.cooldown.contains(e.getPlayer().getName())) {
                return;
            }

            this.cooldown.add(e.getPlayer().getName());
            (new BukkitRunnable() {
                public void run() {
                    if (Cubelets.this.cooldown.contains(e.getPlayer().getName())) {
                        Cubelets.this.cooldown.remove(e.getPlayer().getName());
                    }

                }
            }).runTaskLater(this, 20L);
            if (this.used.contains(this.getFixedLocation(e.getClickedBlock().getLocation()))) {
                e.getPlayer().sendMessage(this.formatText(this.messagesConfig.getConfig().getString("already-in-use")));
                return;
            }

            final Player player = e.getPlayer();
            final Block block = e.getClickedBlock();
            if (this.settingsConfig.getConfig().getBoolean("mysql")) {
                CubeletsAPI.getCubeletsAsync(player).thenAccept(amount -> Bukkit.getScheduler().runTask(this, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (amount < 1) {
                        player.sendMessage(this.formatText(this.messagesConfig.getConfig().getString("not-enough-cubelets")));
                        return;
                    }

                    CubeletsAPI.setCubeletsAsync(player, amount - 1);
                    this.beginCubeletUse(player, block);
                }));
                return;
            }

            if (CubeletsAPI.getCubelets(player) < 1) {
                player.sendMessage(this.formatText(this.messagesConfig.getConfig().getString("not-enough-cubelets")));
                return;
            }

            CubeletsAPI.removeCubelets(player, 1);
            this.beginCubeletUse(player, block);
        }

    }

    private void beginCubeletUse(final Player player, final Block block) {
        this.used.add(this.getFixedLocation(block.getLocation()));
        if (this.holo.containsKey(block)) {
            Hologram h = this.holo.get(block);
            h.delete();
            this.holo.remove(block);
        }

        Location original = this.getFixedLocation(block.getLocation());
        final double o = original.getY();
        this.height.put(block, this.settingsConfig.getConfig().getInt("fireworks-height"));
        (new BukkitRunnable() {
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                } else {
                    int x = (Integer) Cubelets.this.height.get(block);
                    Location max = Cubelets.this.getFixedLocation(block.getLocation()).add(0.0D, (double)x, 0.0D);
                    Cubelets.this.height.put(block, (Integer) Cubelets.this.height.get(block) - 1);
                    if (o > (double)x) {
                        Cubelets.this.LaunchFirework(max);
                    }

                    if (max.getY() == o) {
                        Cubelets.this.reward(player, block);
                    }

                    if (max.getY() <= o) {
                        this.cancel();
                    }
                }
            }
        }).runTaskTimer(this, (long)this.settingsConfig.getConfig().getInt("fireworks-speed"), (long)this.settingsConfig.getConfig().getInt("fireworks-speed"));
    }


    public void reset(final Block b, final Hologram hx, final Hologram hx1) {
        (new BukkitRunnable() {
            public void run() {
                if (!Cubelets.this.hasPotion(b)) {
                    hx.delete();
                    hx1.delete();
                    Cubelets.this.used.remove(Cubelets.this.getFixedLocation(b.getLocation()));
                    Cubelets.this.height.remove(b);
                    this.cancel();
                } else {
                    Hologram h = Cubelets.this.hologramService.createTextHologram(Cubelets.this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D), Cubelets.this.formatText(Cubelets.this.messagesConfig.getConfig().getString("click-to-use")));
                    Cubelets.this.holo.put(b, h);
                    Cubelets.this.used.remove(Cubelets.this.getFixedLocation(b.getLocation()));
                    Cubelets.this.height.remove(b);
                    hx.delete();
                    hx1.delete();
                }
            }
        }).runTaskLater(this, (long)(20 * this.settingsConfig.getConfig().getInt("reset-after")));
    }

    public void reward(Player p, Block b) {
        if (this.size.containsKey(p.getWorld().getName())) {
            int prizessize = this.getConfig().getConfigurationSection(p.getWorld().getName() + ".rewards").getKeys(false).size();
            int random = this.getRandom(0, prizessize);
            if (!this.getConfig().contains(p.getWorld().getName() + ".rewards." + random)) {
                this.reward(p, b);
            } else {
                int chance = this.getRandom(0, this.getConfig().getInt(p.getWorld().getName() + ".max-chance"));
                if (chance > this.getConfig().getInt(p.getWorld().getName() + ".rewards." + random + ".reward-chance")) {
                    this.reward(p, b);
                } else {
                    if (chance <= this.getConfig().getInt(p.getWorld().getName() + ".rewards." + random + ".reward-chance")) {
                        Iterator var7 = this.getConfig().getStringList(p.getWorld().getName() + ".rewards" + "." + random + ".commands").iterator();

                        while(var7.hasNext()) {
                            String s = (String)var7.next();
                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), s.replaceAll("&", "§").replaceAll("%player%", p.getName()));
                        }

                        ItemStack s = null;
                        ItemStack skull;
                        if (this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".item-type").toLowerCase().equalsIgnoreCase("head")) {
                            skull = new ItemStack(Material.SKULL_ITEM, 1, (short)SkullType.PLAYER.ordinal());
                            SkullMeta meta = (SkullMeta)skull.getItemMeta();
                            meta.setOwner(this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".head-name"));
                            skull.setItemMeta(meta);
                            s = skull;
                        }

                        if (this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".item-type").toLowerCase().equalsIgnoreCase("item")) {
                            skull = new ItemStack(Material.getMaterial(this.getConfig().getInt(p.getWorld().getName() + ".rewards" + "." + random + ".reward-item-id")), 1, (short)this.getConfig().getInt(p.getWorld().getName() + ".rewards" + "." + random + ".reward-item-damage"));
                            s = skull;
                        }

                        if (this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".item-type").toLowerCase().equalsIgnoreCase("customhead")) {
                            s = getHead(this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".custom-head-id"));
                        }

                        ArrayList<Player> list = new ArrayList();
                        Iterator var9 = Bukkit.getOnlinePlayers().iterator();

                        while(var9.hasNext()) {
                            Player px = (Player)var9.next();
                            list.add(px);
                        }

                        list.remove(p);
                        Hologram IndiHoloPlayer = this.hologramService.createTextHologram(this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D),
                                this.formatText(this.messagesConfig.getConfig().getString("Individual-Holo-Line-1")
                                        .replaceAll("%reward-name%", this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".reward-name"))));
                        IndiHoloPlayer.getLines().appendText(this.formatText(this.messagesConfig.getConfig().getString("Individual-Holo-Line-2")
                                .replaceAll("%rarity%", this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".reward-rarity"))));
                        this.hologramService.setViewers(IndiHoloPlayer, p);

                        Hologram IndiHoloPlayers = this.hologramService.createTextHologram(this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D),
                                this.formatText(this.messagesConfig.getConfig().getString("Public-Holo-Line-1")
                                        .replaceAll("%player%", p.getName())
                                        .replaceAll("%reward-name%", this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".reward-name"))));
                        IndiHoloPlayers.getLines().appendText(this.formatText(this.messagesConfig.getConfig().getString("Public-Holo-Line-2")
                                .replaceAll("%rarity%", this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".reward-rarity"))));
                        this.hologramService.setViewers(IndiHoloPlayers, list);

                        ArrayList<Color> colors = new ArrayList();
                        ArrayList<Color> fade = new ArrayList();
                        List<String> lore = this.getConfig().getStringList(p.getWorld().getName() + ".rewards" + "." + random + ".firework.colors");
                        List<String> lore2 = this.getConfig().getStringList(p.getWorld().getName() + ".rewards" + "." + random + ".firework.fade");
                        Iterator var16 = lore.iterator();

                        String l;
                        while(var16.hasNext()) {
                            l = (String)var16.next();
                            Color color = this.getColor(l);
                            if (color != null) {
                                colors.add(color);
                            }
                        }

                        var16 = lore2.iterator();

                        while(var16.hasNext()) {
                            l = (String)var16.next();
                            Color color = this.getColor(l);
                            if (color != null) {
                                fade.add(color);
                            }
                        }

                        String rewardTypeName = this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".firework.type");
                        if (rewardTypeName == null || rewardTypeName.trim().isEmpty()) {
                            logThrottled("reward.firework.missing-type." + p.getWorld().getName(), Level.WARNING,
                                    "[Cubelets] Reward firework skipped: missing firework.type for world '" + p.getWorld().getName() + "'.", 30000L);
                            this.reset(b, IndiHoloPlayers, IndiHoloPlayer);
                            p.updateInventory();
                            return;
                        }

                        Type rewardFireworkType;
                        try {
                            rewardFireworkType = Type.valueOf(rewardTypeName.trim().toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            logThrottled("reward.firework.bad-type." + p.getWorld().getName(), Level.WARNING,
                                    "[Cubelets] Reward firework skipped: invalid firework.type '" + rewardTypeName + "' for world '" + p.getWorld().getName() + "'.", 30000L);
                            this.reset(b, IndiHoloPlayers, IndiHoloPlayer);
                            p.updateInventory();
                            return;
                        }

                        final Firework f = (Firework)b.getWorld().spawn(this.getFixedLocation(b.getLocation()), Firework.class);
                        FireworkMeta fm = f.getFireworkMeta();
                        fm.addEffect(FireworkEffect.builder().flicker(this.getConfig().getBoolean(p.getWorld().getName() + ".rewards" + "." + random + ".firework.flicker")).trail(this.getConfig().getBoolean(p.getWorld().getName() + ".rewards" + "." + random + ".firework.trail")).with(rewardFireworkType).withColor(colors).withFade(fade).build());
                        fm.setPower(1);
                        f.setFireworkMeta(fm);
                        (new BukkitRunnable() {
                            public void run() {
                                f.detonate();
                            }
                        }).runTaskLater(getInstance(), 1L);
                        this.reset(b, IndiHoloPlayers, IndiHoloPlayer);
                        p.updateInventory();
                    }

                }
            }
        }
    }

    public void addPotion(Block b) {
        List<String> c = this.dataConfig.getConfig().getStringList("cube-list");
        if (!c.contains(getStringFromLocation(b.getLocation()))) {
            c.add(getStringFromLocation(b.getLocation()));
            this.dataConfig.getConfig().set("cube-list", c);
            this.dataConfig.save();
            Hologram h = this.hologramService.createTextHologram(this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D), this.formatText(this.messagesConfig.getConfig().getString("click-to-use")));
            this.holo.put(b, h);
        }
    }

    public void removePotion(Block b) {
        List<String> c = this.dataConfig.getConfig().getStringList("cube-list");
        if (c.contains(getStringFromLocation(b.getLocation()))) {
            c.remove(getStringFromLocation(b.getLocation()));
            this.dataConfig.getConfig().set("cube-list", c);
            this.dataConfig.save();
            if (this.holo.containsKey(b)) {
                Hologram h = (Hologram)this.holo.get(b);
                h.delete();
                this.holo.remove(b);
            }

        }
    }

    public boolean hasPotion(Block b) {
        List<String> list = this.dataConfig.getConfig().getStringList("cube-list");
        return list.contains(getStringFromLocation(b.getLocation()));
    }

    public static String getStringFromLocation(Location loc) {
        return loc == null ? "" : loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }

    public static Location getLocationFromString(String s) {
        if (s != null && s.trim() != "") {
            String[] parts = s.split(":");
            if (parts.length == 4) {
                World w = Bukkit.getServer().getWorld(parts[0]);
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                return new Location(w, x, y, z);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public String formatText(String s) {
        return s.replaceAll("&", "§");
    }

    public int getRandom(int lower, int upper) {
        return ThreadLocalRandom.current().nextInt(upper - lower + 1) + lower;
    }


    public static ItemStack getHead(String id) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
        SkullMeta headMeta = (SkullMeta)head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", id));
        Field profileField;

        try {
            profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException ex) {
            ex.printStackTrace();
        }

        head.setItemMeta(headMeta);
        return head;
    }

    private void logThrottled(String key, Level level, String message, long intervalMs) {
        long now = System.currentTimeMillis();
        Long last = this.logThrottle.get(key);
        if (last == null || (now - last) >= intervalMs) {
            this.logThrottle.put(key, now);
            this.getLogger().log(level, message);
        }
    }
}
