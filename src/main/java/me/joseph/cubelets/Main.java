package me.joseph.cubelets;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.util.internal.ThreadLocalRandom;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import me.joseph.cubelets.sql.SQLConnection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import me.joseph.cubelets.hologram.HologramService;
import me.filoghost.holographicdisplays.api.hologram.Hologram;

public class Main extends JavaPlugin implements Listener {
    HashMap<String, Integer> size = new HashMap<>();
    public CubeletsAPI api;
    public SQLConnection sqlConnection = null;
    public data data;
    public messages messages;
    public settings settings;
    public static Main instance;
    ArrayList<String> cooldown = new ArrayList<>();
    public ArrayList<Location> used = new ArrayList<>();
    public HashMap<Block, Hologram> holo = new HashMap<>();
    public HashMap<Block, Integer> height = new HashMap<>();
    private HologramService holograms;

    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        this.holograms = new HologramService(this);
        this.settings = new settings(new File(this.getDataFolder() + "/settings.yml"));
        this.settings.save();
        this.settings.getConfig().addDefault("fireworks-height", 15);
        this.settings.getConfig().addDefault("fireworks-speed", 3);
        this.settings.getConfig().addDefault("reset-after", 5);
        this.settings.getConfig().addDefault("floating-item-height", 1.6D);
        this.settings.getConfig().addDefault("mysql", false);
        this.settings.getConfig().addDefault("host", "localhost");
        this.settings.getConfig().addDefault("port", "3306");
        this.settings.getConfig().addDefault("database", "testdb");
        this.settings.getConfig().addDefault("username", "root");
        this.settings.getConfig().addDefault("password", "");
        this.settings.getConfig().options().copyDefaults(true);
        this.settings.save();
        this.data = new data(new File(this.getDataFolder() + "/data.yml"));
        this.data.save();
        this.data.getConfig().options().copyDefaults(true);
        this.data.save();
        this.messages = new messages(new File(this.getDataFolder() + "/messages.yml"));
        this.messages.save();
        this.messages.getConfig().addDefault("add-cubelet-error", "&a[Cubelets] &7You already added this block as a cubelet block!");
        this.messages.getConfig().addDefault("add-cubelet-message", "&a[Cubelets] &eYou have added this block as a cubelet block!");
        this.messages.getConfig().addDefault("remove-cubelet-error", "&a[Cubelets] &7This is not a cubelet block!");
        this.messages.getConfig().addDefault("remove-cubelet-message", "&a[Cubelets] &eYou have removed this cubelet block!");
        this.messages.getConfig().addDefault("already-in-use", "&a[Cubelets] &7This cubelet block is already in use!");
        this.messages.getConfig().addDefault("not-enough-cubelets", "&a[Cubelets] &7You dont have any cubelets to open!");
        this.messages.getConfig().addDefault("not-online", "&a[Cubelets] &7That player is not online!");
        this.messages.getConfig().addDefault("cubelets-add", "&a[Cubelets] &eAdded &a%cubelets% &ecubelets for &a%player%&e!");
        this.messages.getConfig().addDefault("cubelets-remove", "&a[Cubelets] &eRemoved &a%cubelets% &ecubelets from &a%player%&e!");
        this.messages.getConfig().addDefault("cubelets-set", "&a[Cubelets] &eSet cubelets of &a%player% &eto &a%cubelets% &ecubelets!");
        this.messages.getConfig().addDefault("you-have-cubelets-message", "&a[Cubelets] &eYou have &a%amount% &ecubelets!");
        this.messages.getConfig().addDefault("Individual-Holo-Line-1", "&aYou found %reward-name%");
        this.messages.getConfig().addDefault("Individual-Holo-Line-2", "&bRarity: %rarity%");
        this.messages.getConfig().addDefault("Public-Holo-Line-1", "&a%player% has found %reward-name%");
        this.messages.getConfig().addDefault("Public-Holo-Line-2", "&bRarity: %rarity%");
        this.messages.getConfig().addDefault("click-to-use", "&aClick to use");
        this.messages.getConfig().options().copyDefaults(true);
        this.messages.save();
        instance = this;
        this.api = new CubeletsAPI();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        (new BukkitRunnable() {
            public void run() {
                List<String> c = Main.this.data.getConfig().getStringList("cube-list");
                if (c != null && !c.isEmpty()) {
                    for (String s : c) {
                        Location loc = Main.getLocationFromString(s);
                        if (loc == null || loc.getWorld() == null) {
                            continue;
                        }
                        Location holoLoc = Main.this.getFixedLocation(loc.clone().add(0.0D, 2.0D, 0.0D));
                        Hologram h = Main.this.holograms.createTextHologram(holoLoc, Main.this.FormatText(Main.this.messages.getConfig().getString("click-to-use")));
                        Main.this.holo.put(loc.getWorld().getBlockAt(holoLoc.clone().add(0.0D, -2.0D, 0.0D)), h);
                    }
                }
            }
        }).runTaskLater(this, 100L);

        String s;
        if (this.settings.getConfig().getBoolean("mysql")) {
            s = this.settings.getConfig().getString("host");
            String port = this.settings.getConfig().getString("port");
            String database = this.settings.getConfig().getString("database");
            String username = this.settings.getConfig().getString("username");
            String password = this.settings.getConfig().getString("password");
            this.sqlConnection = new SQLConnection(this, s, port, database, username, password);
            this.sqlConnection.openConnection();
        }

        Iterator var7 = Bukkit.getOnlinePlayers().iterator();

        while(var7.hasNext()) {
            Player p = (Player)var7.next();
            this.Register(p);
        }

        var7 = this.getConfig().getConfigurationSection("").getKeys(false).iterator();

        while(var7.hasNext()) {
            s = (String)var7.next();
            if (s != null) {
                this.size.put(s, this.getConfig().getConfigurationSection(s + ".rewards").getKeys(false).size());
            }
        }

    }

    public static Main getInstance() {
        return instance;
    }

    public SQLConnection getMainSQLConnection() {
        return this.sqlConnection;
    }

    public void onDisable() {
        if (this.holograms != null) {
            this.holograms.deleteAllOwnedHolograms();
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
        ArrayList<Color> colors = new ArrayList<>();
        ArrayList<Color> fade = new ArrayList<>();
        List<String> lore = this.getConfig().getStringList(loc.getWorld().getName() + ".firework.colors");
        List<String> lore2 = this.getConfig().getStringList(loc.getWorld().getName() + ".firework.fade");
        Iterator var7 = lore.iterator();

        String l;
        while(var7.hasNext()) {
            l = (String)var7.next();
            colors.add(this.getColor(l));
        }

        var7 = lore2.iterator();

        while(var7.hasNext()) {
            l = (String)var7.next();
            fade.add(this.getColor(l));
        }

        final Firework f = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fm = f.getFireworkMeta();
        fm.addEffect(FireworkEffect.builder().flicker(this.getConfig().getBoolean(loc.getWorld().getName() + ".firework.flicker")).trail(this.getConfig().getBoolean(loc.getWorld().getName() + ".firework.trail")).with(Type.valueOf(this.getConfig().getString(loc.getWorld().getName() + ".firework.type"))).withColor(colors).withFade(fade).build());
        fm.setPower(1);
        f.setFireworkMeta(fm);
        (new BukkitRunnable() {
            public void run() {
                f.detonate();
            }
        }).runTaskLater(getInstance(), 1L);
    }

    public void Register(Player p) {
        if (this.settings.getConfig().getBoolean("mysql") && !this.api.existsInDatabase(p)) {
            this.api.createCubelets(p);
        }

    }

    @EventHandler
    public void onLogin(PlayerJoinEvent e) {
        this.Register(e.getPlayer());
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

            e.getPlayer().sendMessage(this.FormatText(this.messages.getConfig().getString("remove-cubelet-message")));
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
                    if (Main.this.cooldown.contains(e.getPlayer().getName())) {
                        Main.this.cooldown.remove(e.getPlayer().getName());
                    }

                }
            }).runTaskLater(this, 20L);
            if (this.used.contains(this.getFixedLocation(e.getClickedBlock().getLocation()))) {
                e.getPlayer().sendMessage(this.FormatText(this.messages.getConfig().getString("already-in-use")));
                return;
            }

            if (this.api.getCubelets(e.getPlayer()) < 1 || this.api.getCubelets(e.getPlayer()) == 0) {
                e.getPlayer().sendMessage(this.FormatText(this.messages.getConfig().getString("not-enough-cubelets")));
                return;
            }

            this.api.removeCubelets(e.getPlayer(), 1);
            this.used.add(this.getFixedLocation(e.getClickedBlock().getLocation()));
            if (this.holo.containsKey(e.getClickedBlock())) {
                Hologram h = (Hologram)this.holo.get(e.getClickedBlock());
                h.delete();
                this.holo.remove(e.getClickedBlock());
            }

            Location original = this.getFixedLocation(e.getClickedBlock().getLocation());
            final double o = original.getY();
            this.height.put(e.getClickedBlock(), this.settings.getConfig().getInt("fireworks-height"));
            (new BukkitRunnable() {
                public void run() {
                    if (!e.getPlayer().isOnline()) {
                        this.cancel();
                    } else {
                        int x = (Integer)Main.this.height.get(e.getClickedBlock());
                        Location max = Main.this.getFixedLocation(e.getClickedBlock().getLocation()).add(0.0D, (double)x, 0.0D);
                        Main.this.height.put(e.getClickedBlock(), (Integer)Main.this.height.get(e.getClickedBlock()) - 1);
                        if (o > (double)x) {
                            Main.this.LaunchFirework(max);
                        }

                        if (max.getY() == o) {
                            Main.this.reward(e.getPlayer(), e.getClickedBlock());
                        }

                        if (max.getY() <= o) {
                            this.cancel();
                        }
                    }
                }
            }).runTaskTimer(this, (long)this.settings.getConfig().getInt("fireworks-speed"), (long)this.settings.getConfig().getInt("fireworks-speed"));
        }

    }

    public void reset(final Block b, final Hologram hx, final Hologram hx1) {
        (new BukkitRunnable() {
            public void run() {
                if (!Main.this.hasPotion(b)) {
                    hx.delete();
                    hx1.delete();
                    Main.this.used.remove(Main.this.getFixedLocation(b.getLocation()));
                    Main.this.height.remove(b);
                    this.cancel();
                } else {
                    Hologram h = Main.this.holograms.createTextHologram(Main.this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D), Main.this.FormatText(Main.this.messages.getConfig().getString("click-to-use")));
                    Main.this.holo.put(b, h);
                    Main.this.used.remove(Main.this.getFixedLocation(b.getLocation()));
                    Main.this.height.remove(b);
                    hx.delete();
                    hx1.delete();
                }
            }
        }).runTaskLater(this, (long)(20 * this.settings.getConfig().getInt("reset-after")));
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
                        Hologram IndiHoloPlayer = this.holograms.createTextHologram(this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D),
                                this.FormatText(this.messages.getConfig().getString("Individual-Holo-Line-1")
                                        .replaceAll("%reward-name%", this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".reward-name"))));
                        IndiHoloPlayer.getLines().appendText(this.FormatText(this.messages.getConfig().getString("Individual-Holo-Line-2")
                                .replaceAll("%rarity%", this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".reward-rarity"))));
                        this.holograms.setViewers(IndiHoloPlayer, p);

                        Hologram IndiHoloPlayers = this.holograms.createTextHologram(this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D),
                                this.FormatText(this.messages.getConfig().getString("Public-Holo-Line-1")
                                        .replaceAll("%player%", p.getName())
                                        .replaceAll("%reward-name%", this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".reward-name"))));
                        IndiHoloPlayers.getLines().appendText(this.FormatText(this.messages.getConfig().getString("Public-Holo-Line-2")
                                .replaceAll("%rarity%", this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".reward-rarity"))));
                        this.holograms.setViewers(IndiHoloPlayers, list);

                        ArrayList<Color> colors = new ArrayList();
                        ArrayList<Color> fade = new ArrayList();
                        List<String> lore = this.getConfig().getStringList(p.getWorld().getName() + ".rewards" + "." + random + ".firework.colors");
                        List<String> lore2 = this.getConfig().getStringList(p.getWorld().getName() + ".rewards" + "." + random + ".firework.fade");
                        Iterator var16 = lore.iterator();

                        String l;
                        while(var16.hasNext()) {
                            l = (String)var16.next();
                            colors.add(this.getColor(l));
                        }

                        var16 = lore2.iterator();

                        while(var16.hasNext()) {
                            l = (String)var16.next();
                            fade.add(this.getColor(l));
                        }

                        final Firework f = (Firework)b.getWorld().spawn(this.getFixedLocation(b.getLocation()), Firework.class);
                        FireworkMeta fm = f.getFireworkMeta();
                        fm.addEffect(FireworkEffect.builder().flicker(this.getConfig().getBoolean(p.getWorld().getName() + ".rewards" + "." + random + ".firework.flicker")).trail(this.getConfig().getBoolean(p.getWorld().getName() + ".rewards" + "." + random + ".firework.trail")).with(Type.valueOf(this.getConfig().getString(p.getWorld().getName() + ".rewards" + "." + random + ".firework.type"))).withColor(colors).withFade(fade).build());
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
        List<String> c = this.data.getConfig().getStringList("cube-list");
        if (!c.contains(getStringFromLocation(b.getLocation()))) {
            c.add(getStringFromLocation(b.getLocation()));
            this.data.getConfig().set("cube-list", c);
            this.data.save();
            Hologram h = this.holograms.createTextHologram(this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D), this.FormatText(this.messages.getConfig().getString("click-to-use")));
            this.holo.put(b, h);
        }
    }

    public void removePotion(Block b) {
        List<String> c = this.data.getConfig().getStringList("cube-list");
        if (c.contains(getStringFromLocation(b.getLocation()))) {
            c.remove(getStringFromLocation(b.getLocation()));
            this.data.getConfig().set("cube-list", c);
            this.data.save();
            if (this.holo.containsKey(b)) {
                Hologram h = (Hologram)this.holo.get(b);
                h.delete();
                this.holo.remove(b);
            }

        }
    }

    public boolean hasPotion(Block b) {
        List<String> list = this.data.getConfig().getStringList("cube-list");
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

    public String FormatText(String s) {
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
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException var6) {
            var6.printStackTrace();
        }

        head.setItemMeta(headMeta);
        return head;
    }
}
