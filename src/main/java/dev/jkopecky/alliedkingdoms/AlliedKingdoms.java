package dev.jkopecky.alliedkingdoms;

import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.events.ClaimInterferenceListeners;
import dev.jkopecky.alliedkingdoms.events.KingdomEvents;
import dev.jkopecky.alliedkingdoms.util.KingdomUtilMethods;
import io.papermc.paper.util.Tick;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AlliedKingdoms extends JavaPlugin implements Listener {


    public static Economy ECONOMY = null;
    public static Permission PERMISSIONS = null;
    public static Chat CHAT = null;


    @Override
    public void onEnable() {

        //create config file
        saveResource("config.yml", false);

        //economy, perms, and chat setup
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

        //events
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new ClaimInterferenceListeners(), this);
        Bukkit.getPluginManager().registerEvents(new KingdomEvents(), this);

        //init database
        Database.initDatabase();

        //charge kingdoms money based on claims
        final double kingdomChargeDelayMultiplier = getConfig().getDouble("root.kingdom-charge-delay-multiplier", 1);
        long period = (long) (kingdomChargeDelayMultiplier * Tick.tick().fromDuration(Duration.ofDays(1)));
        System.out.println(period);
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, AlliedKingdoms::chargeKingdomsTask, period, period);
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


    private static void chargeKingdomsTask() {
        double costPerChunk = AlliedKingdomsBootstrapper.pluginInstance.getConfig().getDouble("root.kingdom-charge-cost");

        try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
            String sql = "SELECT * FROM kingdoms";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);

            //loop through all kingdoms, deducting the cost from their vault.
            while (resultSet.next()) {

                String kingdom = resultSet.getString("name");
                double vault = resultSet.getDouble("vault");
                String chunkString = resultSet.getString("chunks");
                long chunks = 0;
                if (!chunkString.isEmpty()) {
                    chunks = chunkString.split(",").length;
                }
                double kingdomCost = chunks * costPerChunk;

                //get a list of all members so we can notify them later
                ArrayList<String> members = new ArrayList<>
                        (List.of(resultSet.getString("members")
                        .split(","))
                        .parallelStream().filter(s -> !s.isEmpty())
                        .toList());

                String updateSql = "UPDATE kingdoms SET vault=vault-? WHERE name=?";
                if (vault >= kingdomCost) {
                    //deduct from the kingdom vault
                    PreparedStatement updateStatement = connection.prepareStatement(updateSql);
                    updateStatement.setDouble(1, kingdomCost);
                    updateStatement.setString(2, kingdom);
                    updateStatement.executeUpdate();

                    //increase kingdom accrued value
                    updateSql = "UPDATE kingdoms SET accruedvalue=accruedvalue+? WHERE name=?";
                    updateStatement = connection.prepareStatement(updateSql);
                    updateStatement.setDouble(1, kingdomCost);
                    updateStatement.setString(2, kingdom);
                    updateStatement.executeUpdate();

                    //reset the strike counter
                    boolean strikesReset = false;
                    if (resultSet.getInt("strikes") > 0) {
                        updateSql = "UPDATE kingdoms SET strikes=0 WHERE name=?";
                        updateStatement = connection.prepareStatement(updateSql);
                        updateStatement.setString(1, kingdom);
                        updateStatement.executeUpdate();
                        strikesReset = true;
                    }

                    //notify all online members of the kingdom
                    for (String memberUUID : members) {
                        Player player = AlliedKingdomsBootstrapper.pluginInstance.getServer().getPlayer(UUID.fromString(memberUUID));
                        if (player != null) {
                            player.sendMessage(Component.text("Deducted ", Palette.PRIMARY)
                                    .append(Component.text(kingdomCost, Palette.ACCENT)
                                            .append(Component.text(" from your kingdom's vault (chunk tax)", Palette.PRIMARY))));
                            if (strikesReset) {
                                player.sendMessage(Component.text("Your kingdom's strike counter has been reset to 0", Palette.PRIMARY));
                            }
                        }
                    }
                } else {
                    //insufficient funds to pay
                    int currentStrikes = resultSet.getInt("strikes");
                    currentStrikes++;

                    //deduct from the kingdom vault, sending them into the negative :(
                    PreparedStatement updateStatement = connection.prepareStatement(updateSql);
                    updateStatement.setDouble(1, kingdomCost);
                    updateStatement.setString(2, kingdom);
                    updateStatement.executeUpdate();

                    //notify all online members of the kingdom
                    for (String memberUUID : members) {
                        Player player = AlliedKingdomsBootstrapper.pluginInstance.getServer().getPlayer(UUID.fromString(memberUUID));
                        if (player != null) {
                            player.sendMessage(Component.text("Your kingdom had insufficient funds to pay the chunk tax, and a strike has been issued", Palette.PRIMARY));
                        }
                    }

                    int destitutionLimit = AlliedKingdomsBootstrapper.pluginInstance.getConfig().getInt("root.kingdom-strike-limit-before-destitution");
                    int dissolutionLimit = AlliedKingdomsBootstrapper.pluginInstance.getConfig().getInt("root.kingdom-strike-limit-before-dissolution");
                    if (currentStrikes == destitutionLimit) {
                        //broadcast that the kingdom can no longer defend its territory
                        AlliedKingdomsBootstrapper.pluginInstance.getServer().broadcast(
                                Component.text(kingdom, Palette.ACCENT)
                                .append(Component.text(" is destitute and can no longer effectively defend its territory.", Palette.PRIMARY)));
                    }

                    if (currentStrikes == dissolutionLimit) {
                        //delete the kingdom, inform everyone involved

                        //unclaim all chunks
                        KingdomUtilMethods.unclaimAll(kingdom, AlliedKingdomsBootstrapper.pluginInstance.getServer());

                        //delete kingdom from database
                        String deleteSql = "DELETE FROM kingdoms WHERE name=?";
                        PreparedStatement preparedStatement = connection.prepareStatement(deleteSql);
                        preparedStatement.setString(1, kingdom);
                        preparedStatement.executeUpdate();

                        //inform affected players and delete their kingdom data records
                        KingdomUtilMethods.updateDeletedKingdomPlayers(resultSet.getString("members"));

                        //issue broadcast to the server
                        AlliedKingdomsBootstrapper.pluginInstance.getServer().broadcast(
                                Component.text(kingdom, Palette.ACCENT)
                                .append(Component.text(" has gone bankrupt and been dissolved.", Palette.PRIMARY)));
                    } else {
                        //update kingdom with the new strike value
                        String applyStrikeSQL = "UPDATE kingdoms set strikes=strikes+1 where name=?";
                        PreparedStatement applyStrikeStatement = connection.prepareStatement(applyStrikeSQL);
                        applyStrikeStatement.setString(1, kingdom);
                        applyStrikeStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            AlliedKingdomsBootstrapper.pluginInstance.getLogger().warning("SQL Exception while executing chargeKingdomsTask(): " + e.getMessage());
        }
    }
}
