package dev.jkopecky.alliedkingdoms;

import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.events.ClaimInterferenceListeners;
import dev.jkopecky.alliedkingdoms.events.KingdomEvents;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class AlliedKingdoms extends JavaPlugin implements Listener {


    public static Economy ECONOMY = null;
    public static Permission PERMISSIONS = null;
    public static Chat CHAT = null;


    @Override
    public void onEnable() {

        if (!setupEconomy() ) {
            getLogger().severe("AlliedKingdoms - Vault dependency missing - disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!setupPermissions()) {
            getLogger().info("AlliedKingdoms - No permissions plugin detected");
        }
        if (!setupChat()) {
            getLogger().info("AlliedKingdoms - No chat plugin detected");
        }


        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new ClaimInterferenceListeners(), this);
        Bukkit.getPluginManager().registerEvents(new KingdomEvents(), this);

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



    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            }
            ECONOMY = rsp.getProvider();
            return ECONOMY != null;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp == null) {
            return false;
        }
        CHAT = rsp.getProvider();
        return CHAT != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            return false;
        }
        PERMISSIONS = rsp.getProvider();
        return PERMISSIONS != null;
    }
}
