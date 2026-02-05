package me.joseph.cubelets.api;

import lombok.experimental.UtilityClass;
import me.joseph.cubelets.Cubelets;
import me.joseph.cubelets.sql.SQLConnection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@UtilityClass
public class CubeletsAPI {
    Cubelets plugin = Cubelets.getInstance();

    public SQLConnection getMainSQLConnection() {
        return plugin.sqlConnection;
    }

    public boolean existsInDatabase(Player p) {
        ResultSet result = getMainSQLConnection().executeQuery("SELECT * FROM `Cubelets` WHERE playername='" + p.getName() + "'", false);

        try {
            return result.next();
        } catch (SQLException var4) {
            return false;
        }
    }

    public void createCubelets(Player p) {
        plugin.sqlConnection.executeUpdate("INSERT INTO `Cubelets` (playername, cubelets) VALUES ('" + p.getName() + "', '0');");
    }

    private Integer getCached(Player p) {
        return plugin.getCubeletCache().get(p.getUniqueId());
    }

    private void setCached(Player p, int number) {
        plugin.getCubeletCache().put(p.getUniqueId(), number);
    }

    private void setCubeletsDb(Player p, int number) {
        plugin.sqlConnection.executeUpdate("UPDATE `Cubelets` SET cubelets='" + number + "' WHERE playername='" + p.getName() + "'");
    }

    public void setCubelets(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            plugin.dataConfig.getConfig().set("Cubelets." + p.getUniqueId() + ".cubelet", number);
            plugin.dataConfig.save();
        }

        if (plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            setCached(p, number);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> setCubeletsDb(p, number));
        }

    }

    public int getCubelets(Player p) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return plugin.dataConfig.getConfig().getInt("Cubelets." + p.getUniqueId() + ".cubelet");
        } else if (plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            Integer cached = getCached(p);
            if (cached != null) {
                return cached;
            }
            int res = 0;
            ResultSet result = getMainSQLConnection().executeQuery("SELECT * FROM `Cubelets` WHERE playername='" + p.getName() + "'", false);

            try {
                if (result.next()) {
                    res = Integer.parseInt(result.getString("cubelets"));
                }
            } catch (SQLException var5) {
            }

            return res;
        } else {
            return 0;
        }
    }

    public void addCubelets(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            plugin.dataConfig.getConfig().set("Cubelets." + p.getUniqueId() + ".cubelet", getCubelets(p) + number);
            plugin.dataConfig.save();
        }

        if (plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            int current = getCubelets(p);
            int newAmount = current + number;
            setCubelets(p, newAmount);
        }

    }

    public void removeCubelets(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            plugin.dataConfig.getConfig().set("Cubelets." + p.getUniqueId() + ".cubelet", getCubelets(p) - number);
            plugin.dataConfig.save();
        }

        if (plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            int current = getCubelets(p);
            int newAmount = current - number;
            setCubelets(p, newAmount);
        }

    }

    private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private <T> CompletableFuture<T> supplySync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> existsInDatabaseAsync(Player p) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return supplySync(() -> false);
        }
        return supplyAsync(() -> existsInDatabase(p));
    }

    public CompletableFuture<Void> createCubeletsAsync(Player p) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return supplySync(() -> null);
        }
        return supplyAsync(() -> {
            createCubelets(p);
            return null;
        });
    }

    public CompletableFuture<Void> setCubeletsAsync(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return supplySync(() -> {
                setCubelets(p, number);
                return null;
            });
        }
        setCached(p, number);
        return supplyAsync(() -> {
            setCubeletsDb(p, number);
            return null;
        });
    }

    public CompletableFuture<Integer> getCubeletsAsync(Player p) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return supplySync(() -> getCubelets(p));
        }
        Integer cached = getCached(p);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return supplyAsync(() -> getCubelets(p));
    }

    public CompletableFuture<Void> addCubeletsAsync(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return supplySync(() -> {
                addCubelets(p, number);
                return null;
            });
        }
        return supplyAsync(() -> {
            int current = getCubelets(p);
            int newAmount = current + number;
            setCached(p, newAmount);
            setCubeletsDb(p, newAmount);
            return null;
        });
    }

    public CompletableFuture<Void> removeCubeletsAsync(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return supplySync(() -> {
                removeCubelets(p, number);
                return null;
            });
        }
        return supplyAsync(() -> {
            int current = getCubelets(p);
            int newAmount = current - number;
            setCached(p, newAmount);
            setCubeletsDb(p, newAmount);
            return null;
        });
    }

    public CompletableFuture<Void> flushCubeletsAsync(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return supplySync(() -> {
                setCubelets(p, number);
                return null;
            });
        }
        return supplyAsync(() -> {
            setCubeletsDb(p, number);
            return null;
        });
    }
}
