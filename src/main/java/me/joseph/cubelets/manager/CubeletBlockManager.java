package me.joseph.cubelets.manager;

import lombok.Getter;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.joseph.cubelets.config.DataFile;
import me.joseph.cubelets.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.*;

@Getter
public class CubeletBlockManager {

    private final DataFile data;
    private final Set<Location> usedLocations = new HashSet<>();
    private final Map<Block, Hologram> blockHolograms = new HashMap<>();
    private final Map<Block, Integer> fireworkHeights = new HashMap<>();

    public CubeletBlockManager(DataFile data) {
        this.data = data;
    }

    public void addCubeletBlock(Block block) {
        List<String> cubeList = data.getConfig().getStringList("cube-list");
        String locationStr = LocationUtils.locationToString(block.getLocation());

        if (cubeList.contains(locationStr)) {
            return;
        }

        cubeList.add(locationStr);
        data.getConfig().set("cube-list", cubeList);
        data.save();
    }

    public void removeCubeletBlock(Block block) {
        List<String> cubeList = data.getConfig().getStringList("cube-list");
        String locationStr = LocationUtils.locationToString(block.getLocation());

        if (!cubeList.contains(locationStr)) {
            return;
        }

        cubeList.remove(locationStr);
        data.getConfig().set("cube-list", cubeList);
        data.save();

        Hologram hologram = blockHolograms.remove(block);
        if (hologram != null) {
            hologram.delete();
        }
    }

    public boolean isCubeletBlock(Block block) {
        List<String> cubeList = data.getConfig().getStringList("cube-list");
        return cubeList.contains(LocationUtils.locationToString(block.getLocation()));
    }

    public List<String> getCubeletLocations() {
        return data.getConfig().getStringList("cube-list");
    }

    public void addUsedLocation(Location location) {
        usedLocations.add(location);
    }

    public void removeUsedLocation(Location location) {
        usedLocations.remove(location);
    }

    public boolean isLocationInUse(Location location) {
        return usedLocations.contains(location);
    }

    public void addBlockHologram(Block block, Hologram hologram) {
        blockHolograms.put(block, hologram);
    }

    public Hologram removeBlockHologram(Block block) {
        return blockHolograms.remove(block);
    }

    public void setFireworkHeight(Block block, int height) {
        fireworkHeights.put(block, height);
    }

    public int getFireworkHeight(Block block) {
        return fireworkHeights.getOrDefault(block, 0);
    }

    public void removeFireworkHeight(Block block) {
        fireworkHeights.remove(block);
    }
}

