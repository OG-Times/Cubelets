package me.joseph.cubelets.command;

import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.joseph.cubelets.Cubelets;
import me.joseph.cubelets.api.CubeletsAPI;
import me.joseph.cubelets.config.DataConfig;
import me.joseph.cubelets.config.MessagesConfig;
import me.joseph.cubelets.config.SettingsConfig;
import me.joseph.cubelets.hologram.HologramService;
import me.joseph.cubelets.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * This code was made by jsexp, in case of any unauthorized
 * use, at least please leave credits.
 * Find more about me @ my <a href="https://github.com/hardcorefactions">GitHub</a> :D
 * © 2025 - jsexp
 */
public class CubeletsCommand extends Command {

    private final MessagesConfig messages;
    private final SettingsConfig settings;
    private final DataConfig data;
    private HologramService hologramService;

    public CubeletsCommand(Cubelets cubelets) {
        super("cubelets");

        this.data = cubelets.getDataConfig();
        this.settings = cubelets.getSettingsConfig();
        this.messages = cubelets.getMessagesConfig();
        this.hologramService = cubelets.getHologramService();
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (label.equalsIgnoreCase("cubelets")) {
            if (sender instanceof ConsoleCommandSender) {
                int parse;
                if (args[0].equalsIgnoreCase("add")) {
                    if (Bukkit.getPlayer(args[1]) == null) {
                        return true;
                    }

                    parse = Integer.parseInt(args[2]);
                    CubeletsAPI.addCubelets(Bukkit.getPlayer(args[1]), parse);
                }

                if (args[0].equalsIgnoreCase("set")) {
                    if (Bukkit.getPlayer(args[1]) == null) {
                        return true;
                    }

                    parse = Integer.parseInt(args[2]);
                    CubeletsAPI.setCubelets(Bukkit.getPlayer(args[1]), parse);
                }

                if (args[0].equalsIgnoreCase("remove")) {
                    if (Bukkit.getPlayer(args[1]) == null) {
                        return true;
                    }

                    parse = Integer.parseInt(args[2]);
                    if (CubeletsAPI.getCubelets(Bukkit.getPlayer(args[1])) < parse) {
                        CubeletsAPI.setCubelets(Bukkit.getPlayer(args[1]), 0);
                        return true;
                    }

                    CubeletsAPI.removeCubelets(Bukkit.getPlayer(args[1]), parse);
                }
            }

            if (sender instanceof Player player) {
                int parse;
                if (args.length == 0) {
                    if (!player.isOp() && !player.hasPermission("cubelets.admin")) {
                        player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("you-have-cubelets-message").replaceAll("%amount%", String.valueOf(CubeletsAPI.getCubelets(player)))));
                        return true;
                    }

                    if (player.isOp() && player.hasPermission("cubelets.admin")) {
                        player.sendMessage(ChatColor.GREEN + "This plugin was made by JosephGP");
                        player.sendMessage(ChatColor.GRAY + "This message can only be seen by op players");
                        player.sendMessage("" + ChatColor.WHITE);
                        player.sendMessage(ChatColor.AQUA + "/cubelets " + ChatColor.GRAY + "| " + ChatColor.WHITE + "Cubelets command");
                        player.sendMessage(ChatColor.AQUA + "/cubelets addcubelet " + ChatColor.GRAY + "| " + ChatColor.WHITE + "Add target block as a cubelet");
                        player.sendMessage(ChatColor.AQUA + "/cubelets removecubelet " + ChatColor.GRAY + "| " + ChatColor.WHITE + "Remove target cubelet block");
                        player.sendMessage(ChatColor.AQUA + "/cubelets <add,set,remove> <player> <amount> " + ChatColor.GRAY + "| " + ChatColor.WHITE + "Manage players cubelets");
                        player.sendMessage(ChatColor.AQUA + "/cubelets reload " + ChatColor.GRAY + "| " + ChatColor.WHITE + "Reload config file");
                        return true;
                    }
                }

                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    if (!player.isOp() && !player.hasPermission("cubelets.admin")) {
                        return true;
                    }

                    if (Cubelets.getInstance().getSize() != null && !Cubelets.getInstance().getSize().isEmpty()) {
                        Cubelets.getInstance().getSize().clear();
                    }

                    this.settings.reload();
                    this.messages.reload();
                    Cubelets.getInstance().reloadConfig();

                    for (String s : Cubelets.getInstance().getConfig().getConfigurationSection("").getKeys(false)) {
                        if (s != null) {
                            Cubelets.getInstance().getSize().put(s, Cubelets.getInstance().getConfig().getConfigurationSection(s + ".rewards").getKeys(false).size());
                        }
                    }

                    player.sendMessage(ChatColor.GREEN + "Cubelets config reloaded!");
                }

                if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("add") && sender instanceof Player) {
                        if (!player.isOp() && !player.hasPermission("cubelets.admin")) {
                            return true;
                        }

                        if (Bukkit.getPlayer(args[1]) == null) {
                            player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("not-online")));
                            return true;
                        }

                        parse = Integer.parseInt(args[2]);
                        CubeletsAPI.addCubelets(Bukkit.getPlayer(args[1]), parse);
                        player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("cubelets-add").replaceAll("%cubelets%", String.valueOf(parse)).replaceAll("%player%", Bukkit.getPlayer(args[1]).getName())));
                    }

                    if (args[0].equalsIgnoreCase("remove") && sender instanceof Player) {
                        if (!player.isOp() && !player.hasPermission("cubelets.admin")) {
                            return true;
                        }

                        if (Bukkit.getPlayer(args[1]) == null) {
                            player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("not-online")));
                            return true;
                        }

                        parse = Integer.parseInt(args[2]);
                        CubeletsAPI.removeCubelets(Bukkit.getPlayer(args[1]), parse);
                        player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("cubelets-remove").replaceAll("%cubelets%", String.valueOf(parse)).replaceAll("%player%", Bukkit.getPlayer(args[1]).getName())));
                    }

                    if (args[0].equalsIgnoreCase("set")) {
                        if (!player.isOp() && !player.hasPermission("cubelets.admin")) {
                            return true;
                        }

                        if (Bukkit.getPlayer(args[1]) == null) {
                            player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("not-online")));
                            return true;
                        }

                        parse = Integer.parseInt(args[2]);
                        CubeletsAPI.setCubelets(Bukkit.getPlayer(args[1]), parse);
                        player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("cubelets-set").replaceAll("%cubelets%", String.valueOf(parse)).replaceAll("%player%", Bukkit.getPlayer(args[1]).getName())));
                    }
                }

                if (args.length == 1) {
                    Block block;
                    if (args[0].equalsIgnoreCase("addcubelet")) {
                        if (!player.isOp() && !player.hasPermission("cubelets.admin")) {
                            return true;
                        }

                        block = player.getTargetBlock((Set<Material>) null, 20);
                        if (block.getType() != Material.AIR) {
                            if (this.hasPotion(block)) {
                                player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("add-cubelet-error")));
                                return true;
                            }

                            if (!this.hasPotion(block)) {
                                this.addPotion(block);
                                player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("add-cubelet-message")));
                            }
                        }
                    }

                    if (args[0].equalsIgnoreCase("removecubelet")) {
                        if (!player.isOp() && !player.hasPermission("cubelets.admin")) {
                            return true;
                        }

                        block = player.getTargetBlock((Set<Material>) null, 20);
                        if (block.getType() != Material.AIR) {
                            if (!this.hasPotion(block)) {
                                player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("remove-cubelet-error")));
                                return true;
                            }

                            if (this.hasPotion(block)) {
                                player.sendMessage(ColorUtil.colorize(this.messages.getConfig().getString("remove-cubelet-message")));
                                this.removePotion(block);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public void addPotion(Block b) {
        List<String> c = this.data.getConfig().getStringList("cube-list");
        if (!c.contains(getStringFromLocation(b.getLocation()))) {
            c.add(getStringFromLocation(b.getLocation()));
            this.data.getConfig().set("cube-list", c);
            this.data.save();
            Hologram h = this.hologramService.createTextHologram(this.getFixedLocation(b.getLocation()).add(0.0D, 2.0D, 0.0D), ColorUtil.colorize(this.messages.getConfig().getString("click-to-use")));
            Cubelets.getInstance().holo.put(b, h);
        }
    }

    public Location getFixedLocation(Location loc) {
        return loc.add(0.5D, 0.0D, 0.5D);
    }

    public boolean hasPotion(Block b) {
        List<String> list = this.data.getConfig().getStringList("cube-list");
        return list.contains(getStringFromLocation(b.getLocation()));
    }

    public void removePotion(Block b) {
        List<String> c = this.data.getConfig().getStringList("cube-list");
        if (c.contains(getStringFromLocation(b.getLocation()))) {
            c.remove(getStringFromLocation(b.getLocation()));
            this.data.getConfig().set("cube-list", c);
            this.data.save();
            if (Cubelets.getInstance().holo.containsKey(b)) {
                Hologram h = Cubelets.getInstance().holo.get(b);
                h.delete();
                Cubelets.getInstance().holo.remove(b);
            }

        }
    }

    public static String getStringFromLocation(Location loc) {
        return loc == null ? "" : loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }

}