package dev.jkopecky.alliedkingdoms.events;

import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import dev.jkopecky.alliedkingdoms.util.KingdomUtilMethods;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.*;

public class ClaimInterferenceListeners implements Listener {


    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        //check if block is below configured min y level
        if (event.getBlock().getY() < AlliedKingdomsBootstrapper.pluginInstance.getConfig().getInt("chunks.protection-min-y")) {
            return;
        }

        Chunk chunk = event.getBlock().getChunk();
        PersistentDataContainer chunkContainer = chunk.getPersistentDataContainer();
        NamespacedKey chunkKey = PDCDataKeys.getChunkKingdomKey();

        //stop early if the block isn't placed in an owned chunk
        if (!chunkContainer.has(chunkKey, PersistentDataType.STRING)) {
            return;
        }
        String chunkKingdom = chunkContainer.get(chunkKey, PersistentDataType.STRING);

        //get player kingdom
        Player player = event.getPlayer();
        PersistentDataContainer playerContainer = player.getPersistentDataContainer();
        NamespacedKey playerKey = PDCDataKeys.getPlayerKingdomKey();
        String playerKingdom = "";
        if (playerContainer.has(playerKey, PersistentDataType.STRING)) {
            playerKingdom = playerContainer.get(playerKey, PersistentDataType.STRING);
        }

        if (playerKingdom.equals(chunkKingdom)) {
            return;
        }

        if (!KingdomUtilMethods.isKingdomDestitute(chunkKingdom)) {
            //player is not allowed to place blocks in this chunk
            player.sendMessage(Component.text("You are not allowed to build in chunks owned by " + chunkKingdom, Palette.ERROR));
            event.setCancelled(true);
        }
    }



    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        //check if block is below configured min y level
        if (event.getBlock().getY() < AlliedKingdomsBootstrapper.pluginInstance.getConfig().getInt("chunks.protection-min-y")) {
            return;
        }

        Chunk chunk = event.getBlock().getChunk();
        PersistentDataContainer chunkContainer = chunk.getPersistentDataContainer();
        NamespacedKey chunkKey = PDCDataKeys.getChunkKingdomKey();

        //stop early if the block isn't broken in an owned chunk
        if (!chunkContainer.has(chunkKey, PersistentDataType.STRING)) {
            return;
        }
        String chunkKingdom = chunkContainer.get(chunkKey, PersistentDataType.STRING);

        //get player kingdom
        Player player = event.getPlayer();
        PersistentDataContainer playerContainer = player.getPersistentDataContainer();
        NamespacedKey playerKey = PDCDataKeys.getPlayerKingdomKey();
        String playerKingdom = "";
        if (playerContainer.has(playerKey, PersistentDataType.STRING)) {
            playerKingdom = playerContainer.get(playerKey, PersistentDataType.STRING);
        }

        if (!playerKingdom.equals(chunkKingdom)) {
            //player is not allowed to break blocks in this chunk under normal circumstances
            //check destitution state
            if (!KingdomUtilMethods.isKingdomDestitute(chunkKingdom)) {

                //check if player is using a valid raid item
                PersistentDataContainer dataContainer = player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer();
                if (dataContainer.has(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING)) {
                    String raidItemData = dataContainer.get(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING);
                    if (raidItemData.contains("raiditem")) {
                        return;
                    }
                }
                player.sendMessage(Component.text("You are not allowed to break blocks in chunks owned by " + chunkKingdom, Palette.ERROR));
                event.setCancelled(true);
                return;
            }
        }

        //check that the blocks are not the throne
        Block block = event.getBlock();
        Block gold;
        boolean isGold = block.getType().name().equals("GOLD_BLOCK");
        boolean isUnderStairs = block.getRelative(BlockFace.UP, 1).getType().name().contains("STAIRS");
        boolean isStairs = block.getType().name().contains("STAIRS");
        boolean isAboveGold = block.getRelative(BlockFace.DOWN, 1).getType().name().equals("GOLD_BLOCK");
        if (isGold && isUnderStairs || isStairs && isAboveGold) {
            if (isGold) {
                gold = block;
            } else {
                gold = block.getRelative(BlockFace.DOWN, 1);
            }

            //might be a throne. Need to check the database to see if the kingdom's throne is at that location
            try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {

                String sql = "SELECT * FROM kingdoms WHERE name=?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, playerKingdom);
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    String serializedThrone = result.getString("throne");
                    if (serializedThrone.isEmpty()) {
                        return;
                    }

                    String[] throneBlockCoords = serializedThrone.split("&throne=")[1].split(",");
                    if (gold.getX() == Integer.parseInt(throneBlockCoords[0])
                            && gold.getY() == Integer.parseInt(throneBlockCoords[1])
                            && gold.getZ() == Integer.parseInt(throneBlockCoords[2])) {
                        //player is attempting to break their kingdom's throne. Not permitted.
                        player.sendMessage(Component.text("You cannot break the kingdom's throne until a new one is created", Palette.PRIMARY));
                        event.setCancelled(true);
                    }
                } else {
                    player.sendMessage(Component.text("Your kingdom was not found in the database. Contact the plugin developer. Check the logs.", Palette.ERROR));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
