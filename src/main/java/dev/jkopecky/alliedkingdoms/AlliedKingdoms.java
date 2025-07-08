package dev.jkopecky.alliedkingdoms;

import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.events.ClaimInterferenceListeners;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.*;

public class AlliedKingdoms extends JavaPlugin implements Listener {


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new ClaimInterferenceListeners(), this);

        Database.initDatabase();
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));

        //add player to database so we can keep track of their uuid
        try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
            String sql = "INSERT INTO players (name, uuid) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, event.getPlayer().getName());
            statement.setString(2, event.getPlayer().getUniqueId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            //todo logging
            e.printStackTrace();
        }
    }
}
