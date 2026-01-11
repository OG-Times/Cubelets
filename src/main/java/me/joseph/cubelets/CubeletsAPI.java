package me.joseph.cubelets;

import java.sql.ResultSet;
import java.sql.SQLException;
import me.joseph.cubelets.sql.SQLConnection;
import org.bukkit.entity.Player;

public class CubeletsAPI {
    Main plugin = Main.getInstance();

    public SQLConnection getMainSQLConnection() {
        return this.plugin.sqlConnection;
    }

    public boolean existsInDatabase(Player p) {
        ResultSet result = this.getMainSQLConnection().executeQuery("SELECT * FROM `Cubelets` WHERE playername='" + p.getName() + "'", false);

        try {
            return result.next();
        } catch (SQLException var4) {
            return false;
        }
    }

    public void createCubelets(Player p) {
        this.plugin.sqlConnection.executeUpdate("INSERT INTO `Cubelets` (playername, cubelets) VALUES ('" + p.getName() + "', '0');");
    }

    public void setCubelets(Player p, int number) {
        if (!this.plugin.settings.getConfig().getBoolean("mysql")) {
            this.plugin.data.getConfig().set("Cubelets." + p.getUniqueId() + ".cubelet", number);
            this.plugin.data.save();
        }

        if (this.plugin.settings.getConfig().getBoolean("mysql")) {
            this.plugin.sqlConnection.executeUpdate("UPDATE `Cubelets` SET cubelets='" + number + "' WHERE playername='" + p.getName() + "'");
        }

    }

    public int getCubelets(Player p) {
        if (!this.plugin.settings.getConfig().getBoolean("mysql")) {
            return this.plugin.data.getConfig().getInt("Cubelets." + p.getUniqueId() + ".cubelet");
        } else if (this.plugin.settings.getConfig().getBoolean("mysql")) {
            int res = 0;
            ResultSet result = this.getMainSQLConnection().executeQuery("SELECT * FROM `Cubelets` WHERE playername='" + p.getName() + "'", false);

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
        if (!this.plugin.settings.getConfig().getBoolean("mysql")) {
            this.plugin.data.getConfig().set("Cubelets." + p.getUniqueId() + ".cubelet", this.getCubelets(p) + number);
            this.plugin.data.save();
        }

        if (this.plugin.settings.getConfig().getBoolean("mysql")) {
            int newAmount = this.getCubelets(p) + number;
            this.setCubelets(p, newAmount);
        }

    }

    public void removeCubelets(Player p, int number) {
        if (!this.plugin.settings.getConfig().getBoolean("mysql")) {
            this.plugin.data.getConfig().set("Cubelets." + p.getUniqueId() + ".cubelet", this.getCubelets(p) - number);
            this.plugin.data.save();
        }

        if (this.plugin.settings.getConfig().getBoolean("mysql")) {
            int newAmount = this.getCubelets(p) - number;
            this.setCubelets(p, newAmount);
        }

    }
}
