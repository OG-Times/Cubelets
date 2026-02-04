package me.joseph.cubelets.api;

import lombok.experimental.UtilityClass;
import me.joseph.cubelets.Cubelets;
import me.joseph.cubelets.sql.SQLConnection;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;

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

    public void setCubelets(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            plugin.dataConfig.getConfig().set("Cubelets." + p.getUniqueId() + ".cubelet", number);
            plugin.dataConfig.save();
        }

        if (plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            plugin.sqlConnection.executeUpdate("UPDATE `Cubelets` SET cubelets='" + number + "' WHERE playername='" + p.getName() + "'");
        }

    }

    public int getCubelets(Player p) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            return plugin.dataConfig.getConfig().getInt("Cubelets." + p.getUniqueId() + ".cubelet");
        } else if (plugin.settingsConfig.getConfig().getBoolean("mysql")) {
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
            int newAmount = getCubelets(p) + number;
            setCubelets(p, newAmount);
        }

    }

    public void removeCubelets(Player p, int number) {
        if (!plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            plugin.dataConfig.getConfig().set("Cubelets." + p.getUniqueId() + ".cubelet", getCubelets(p) - number);
            plugin.dataConfig.save();
        }

        if (plugin.settingsConfig.getConfig().getBoolean("mysql")) {
            int newAmount = getCubelets(p) - number;
            setCubelets(p, newAmount);
        }

    }
}
