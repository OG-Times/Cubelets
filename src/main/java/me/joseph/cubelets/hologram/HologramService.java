package me.joseph.cubelets.hologram;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.VisibilitySettings;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class HologramService {

    private final Plugin plugin;

    public HologramService(Plugin plugin) {
        this.plugin = plugin;
    }

    public Hologram createTextHologram(Location loc, List<String> lines) {
        Hologram hologram = HolographicDisplaysAPI.get(plugin).createHologram(loc);
        if (lines != null) {
            for (String line : lines) {
                hologram.getLines().appendText(line);
            }
        }
        return hologram;
    }

    public Hologram createTextHologram(Location loc, String... lines) {
        return createTextHologram(loc, lines == null ? null : Arrays.asList(lines));
    }

    public void setViewers(Hologram hologram, Player only) {
        if (hologram == null || only == null) return;
        VisibilitySettings visibility = hologram.getVisibilitySettings();
        visibility.setGlobalVisibility(VisibilitySettings.Visibility.HIDDEN);
        visibility.setIndividualVisibility(only, VisibilitySettings.Visibility.VISIBLE);
    }

    public void setViewers(Hologram hologram, Collection<? extends Player> viewers) {
        if (hologram == null || viewers == null) return;
        VisibilitySettings visibility = hologram.getVisibilitySettings();
        visibility.setGlobalVisibility(VisibilitySettings.Visibility.HIDDEN);
        for (Player viewer : viewers) {
            if (viewer != null) {
                visibility.setIndividualVisibility(viewer, VisibilitySettings.Visibility.VISIBLE);
            }
        }
    }

    public void deleteAllOwnedHolograms() {
        for (Hologram h : HolographicDisplaysAPI.get(plugin).getHolograms()) {
            h.delete();
        }
    }
}

