package dev.jkopecky.alliedkingdoms.util;

import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.*;
import java.util.UUID;

public class KingdomUtilMethods {




    public static void unclaimAll(String kingdom, Server server) throws SQLException {
        Connection connection = DriverManager.getConnection(Database.databaseUrl);

        //retreive chunks
        String chunks = "";
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM kingdoms WHERE name=?");
        statement.setString(1, kingdom);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            chunks = result.getString("chunks");
        }

        //unclaim all chunks
        if (!chunks.isEmpty()) {
            NamespacedKey chunkKingdomKey = PDCDataKeys.getChunkKingdomKey();
            World world = server.getWorlds().getFirst();
            for (String chunkId : chunks.split(",")) {
                try {
                    long chunkKey = Long.parseLong(chunkId);
                    PersistentDataContainer container = world.getChunkAt(chunkKey).getPersistentDataContainer();
                    if (container.has(chunkKingdomKey, PersistentDataType.STRING)) {
                        String chunkKingdom = container.get(chunkKingdomKey, PersistentDataType.STRING);
                        if (chunkKingdom.equals(kingdom)) {
                            container.remove(chunkKingdomKey);
                        }
                    }
                } catch (NumberFormatException e) {}
            }
        }

        connection.close();
    }



    public static void updateDeletedKingdomPlayers(String members) {
        for (String playerUUID : members.split(",")) {

            Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));
            if (player == null) continue; //true if player offline

            PersistentDataContainer container = player.getPersistentDataContainer();
            if (container.has(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING)) {
                container.remove(PDCDataKeys.getPlayerKingdomKey());
            }

            player.sendMessage(Component.text("Your kingdom has been dissolved", Palette.PRIMARY));
        }
    }



    public static boolean isKingdomDestitute(String playerKingdom) {
        //kingdom might be destitute
        try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
            String sql = "SELECT * FROM kingdoms WHERE name=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, playerKingdom);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                int strikes = result.getInt("strikes");
                if (strikes >= 3) {
                    //kingdom is destitute and claim protection is disabled
                    return true;
                }
            }
        } catch (SQLException ignored) {}
        return false;
    }
}
