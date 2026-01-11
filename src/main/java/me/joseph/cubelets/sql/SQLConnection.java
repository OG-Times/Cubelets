package me.joseph.cubelets.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import me.joseph.cubelets.Main;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SQLConnection {
    public SQLDatabase MySQL;
    public static Connection c;

    public SQLConnection(Plugin plugin, String host, String port, String database, String username, String password) {
        this.MySQL = new SQLDatabase(plugin, host, port, database, username, password);
    }

    public void openConnection() {
        if (this.isConnected()) {
            this.closeConnection();
        }

        try {
            c = this.MySQL.openConnection();
            this.executeUpdate("CREATE TABLE IF NOT EXISTS `Cubelets` (playername VARCHAR(64), cubelets INT(10));");
            (new BukkitRunnable() {
                public void run() {
                    SQLConnection.this.openConnection();
                }
            }).runTaskLater(Main.getInstance(), 100L);
        } catch (ClassNotFoundException var2) {
            var2.printStackTrace();
        } catch (SQLException var3) {
            var3.printStackTrace();
        }

    }

    public boolean isConnected() {
        try {
            return !c.isClosed();
        } catch (Exception var2) {
            return false;
        }
    }

    public void closeConnection() {
        if (this.isConnected()) {
            try {
                c.close();
            } catch (SQLException var2) {
            }
        }

    }

    public ResultSet executeQuery(String statement, boolean next) {
        if (this.isConnected()) {
            try {
                Statement s = c.createStatement();
                ResultSet res = s.executeQuery(statement);
                if (next) {
                    res.next();
                }

                return res;
            } catch (SQLException var5) {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean executeUpdate(String statement) {
        if (this.isConnected()) {
            try {
                Statement s = c.createStatement();
                s.executeUpdate(statement);
                return true;
            } catch (SQLException var3) {
                var3.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
}
