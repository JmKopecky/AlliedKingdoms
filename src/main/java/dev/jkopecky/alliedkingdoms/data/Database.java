package dev.jkopecky.alliedkingdoms.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {


    private static final String databaseUrl = "jdbc:sqlite:plugins/AlliedKingdoms/database.db";
    private static final String factionTableSQL =
            "CREATE TABLE IF NOT EXISTS kingdoms (" +
            "id INTEGER PRIMARY KEY," +
            "name TEXT," +
            "owner TEXT," +
            "tagline TEXT," +
            "peaceful BOOLEAN" +
            ");";


    public static void initDatabase() {
        try {
            //load driver
            Class.forName("org.sqlite.JDBC");

            //open connection
            Connection connection = DriverManager.getConnection(databaseUrl);
            Statement statement = connection.createStatement();

            //create tables if they do not already exist
            statement.execute(factionTableSQL);

            //close connections
            connection.close();

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
}
