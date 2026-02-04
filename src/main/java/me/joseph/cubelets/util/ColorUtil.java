package me.joseph.cubelets.util;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

/**
 * This code was made by jsexp, in case of any unauthorized
 * use, at least please leave credits.
 * Find more about me @ my <a href="https://github.com/hardcorefactions">GitHub</a> :D
 * © 2025 - jsexp
 */
@UtilityClass
public class ColorUtil {

    public String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

}
