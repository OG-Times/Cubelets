package me.joseph.cubelets.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class LocationUtils {

    private static final String LOCATION_SEPARATOR = ":";

    public static Location centerLocation(Location location) {
        return location.clone().add(0.5, 0, 0.5);
    }

    public static String formatText(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static int getRandomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static String locationToString(Location location) {
        if (location == null) {
            return "";
        }
        return String.format("%s%s%.2f%s%.2f%s%.2f",
                location.getWorld().getName(), LOCATION_SEPARATOR,
                location.getX(), LOCATION_SEPARATOR,
                location.getY(), LOCATION_SEPARATOR,
                location.getZ());
    }

    public static Location parseLocation(String locationStr) {
        if (locationStr == null || locationStr.trim().isEmpty()) {
            return null;
        }

        String[] parts = locationStr.trim().split(LOCATION_SEPARATOR);
        if (parts.length != 4) {
            return null;
        }

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static List<Color> parseColors(List<String> colorNames) {
        return colorNames.stream()
                .map(LocationUtils::parseColor)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Color parseColor(String colorName) {
        if (colorName == null) return null;

        return switch (colorName.toUpperCase()) {
            case "AQUA" -> Color.AQUA;
            case "BLACK" -> Color.BLACK;
            case "BLUE" -> Color.BLUE;
            case "FUCHSIA" -> Color.FUCHSIA;
            case "GRAY" -> Color.GRAY;
            case "GREEN" -> Color.GREEN;
            case "LIME" -> Color.LIME;
            case "MAROON" -> Color.MAROON;
            case "NAVY" -> Color.NAVY;
            case "OLIVE" -> Color.OLIVE;
            case "ORANGE" -> Color.ORANGE;
            case "PURPLE" -> Color.PURPLE;
            case "RED" -> Color.RED;
            case "SILVER" -> Color.SILVER;
            case "TEAL" -> Color.TEAL;
            case "WHITE" -> Color.WHITE;
            case "YELLOW" -> Color.YELLOW;
            default -> null;
        };
    }
}

