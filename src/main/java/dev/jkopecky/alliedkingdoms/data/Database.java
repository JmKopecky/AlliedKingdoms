package dev.jkopecky.alliedkingdoms.data;

import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {


    public static final String databaseUrl = "jdbc:sqlite:plugins/AlliedKingdoms/database.db";
    private static final String factionTableSQL =
            "CREATE TABLE IF NOT EXISTS kingdoms (" +
            "id INTEGER AUTO_INCREMENT," +
            "name TEXT," +
            "owner TEXT," +
            "tagline TEXT," +
            "peaceful BOOLEAN," +
            "members TEXT," +
            "chunks TEXT," +
            "throne TEXT," +
            "vault NUMERIC," +
            "accruedvalue NUMERIC," +
            "strikes INTEGER," +
            "PRIMARY KEY (id)" +
            ");";
    private static final String playerTableSQL =
            "CREATE TABLE IF NOT EXISTS players ("
            + "id INTEGER AUTO_INCREMENT,"
            + "name TEXT,"
            + "uuid TEXT,"
            + "PRIMARY KEY (id));";


    public static void initDatabase() {
        try {
            //load driver
            Class.forName("org.sqlite.JDBC");

            //open connection
            Connection connection = DriverManager.getConnection(databaseUrl);
            Statement statement = connection.createStatement();

            //create tables if they do not already exist
            statement.execute(factionTableSQL);
            statement = connection.createStatement();
            statement.execute(playerTableSQL);

            //close connections
            connection.close();

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
}
