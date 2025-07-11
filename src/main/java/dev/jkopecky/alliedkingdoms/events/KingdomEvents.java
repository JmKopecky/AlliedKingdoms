package dev.jkopecky.alliedkingdoms.events;

import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import io.papermc.paper.util.Tick;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.*;

public class KingdomEvents implements Listener {


    @EventHandler()
    public void onPlayerMove(PlayerMoveEvent event) {

        //check that the player is between two different chunks
        Chunk from = event.getFrom().getChunk();
        Chunk to = event.getTo().getChunk();
        if (from == to) {
            return;
        }

        //get chunk kingdoms
        NamespacedKey chunkKey = PDCDataKeys.getChunkKingdomKey();
        PersistentDataContainer fromContainer = from.getPersistentDataContainer();
        String fromKingdom = "";
        PersistentDataContainer toContainer = to.getPersistentDataContainer();
        String toKingdom = "";
        if (fromContainer.has(chunkKey, PersistentDataType.STRING)) {
            fromKingdom = fromContainer.get(chunkKey, PersistentDataType.STRING);
        }
        if (toContainer.has(chunkKey, PersistentDataType.STRING)) {
            toKingdom = toContainer.get(chunkKey, PersistentDataType.STRING);
        }

        //check moving between kingdoms
        if (fromKingdom.equals(toKingdom)) {
            return;
        }

        //send player the title on their screen
        Player player = event.getPlayer();
        Title title;
        if (toKingdom.isEmpty()) {
            title = Title.title(
                    Component.empty(), Component.text("Wilderness", Palette.WILDERNESS_SUBTITLE, TextDecoration.BOLD),
                    Title.Times.times(Tick.of(10), Tick.of(30), Tick.of(10)));
        } else {
            title = Title.title(
                    Component.empty(), Component.text(toKingdom, Palette.ACCENT, TextDecoration.BOLD),
                    Title.Times.times(Tick.of(10), Tick.of(30), Tick.of(10)));
        }
        player.showTitle(title);
    }


    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Component line = event.line(0);
        if (line == null) {
            return;
        }
        String content = ((TextComponent) line).content();

        if (!content.equals("throne")) {
            return;
        }

        //make sure the sign is next to a gold block
        Block north = event.getBlock().getRelative(BlockFace.NORTH, 1);
        Block south = event.getBlock().getRelative(BlockFace.SOUTH, 1);
        Block west = event.getBlock().getRelative(BlockFace.WEST, 1);
        Block east = event.getBlock().getRelative(BlockFace.EAST, 1);
        Block goldBlock;
        if (north.getType().name().equals("GOLD_BLOCK")) {
            goldBlock = north;
        } else if (south.getType().name().equals("GOLD_BLOCK")) {
            goldBlock = south;
        } else if (west.getType().name().equals("GOLD_BLOCK")) {
            goldBlock = west;
        } else if (east.getType().name().equals("GOLD_BLOCK")) {
            goldBlock = east;
        } else {
            return;
        }

        //check that the block above the gold is some kind of stair
        Block above = goldBlock.getRelative(BlockFace.UP, 1);
        if (!above.getType().name().contains("STAIRS")) {
            return;
        }

        //check if the sign is placed in a chunk claimed by the player's kingdom
        PersistentDataContainer chunkData = event.getBlock().getChunk().getPersistentDataContainer();
        PersistentDataContainer playerData = event.getPlayer().getPersistentDataContainer();
        if (chunkData.has(PDCDataKeys.getChunkKingdomKey(), PersistentDataType.STRING)
                && playerData.has(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING)) {
            if (!chunkData.get(PDCDataKeys.getChunkKingdomKey(), PersistentDataType.STRING).equals(
                    playerData.get(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING))) {
                event.getPlayer().sendMessage(Component.text("You can only create a throne in your kingdom's territory", Palette.ERROR));
                return;
            }
        } else {
            event.getPlayer().sendMessage(Component.text("You can only create a throne in your kingdom's territory", Palette.ERROR));
            return;
        }

        //create throne
        createThrone(event.getBlock().getLocation(), event.getPlayer(), goldBlock);
    }



    private static void createThrone(Location tpLoc, Player player, Block gold) {
        PersistentDataContainer playerContainer = player.getPersistentDataContainer();
        NamespacedKey playerKey = PDCDataKeys.getPlayerKingdomKey();

        String kingdom;
        if (playerContainer.has(playerKey, PersistentDataType.STRING)) {
            kingdom = playerContainer.get(playerKey, PersistentDataType.STRING);
        } else {
            player.sendMessage(Component.text("You must be part of a kingdom to create a throne", Palette.ERROR));
            return;
        }

        String throneEntry = serializeThrone(tpLoc, gold);

        Server server = Bukkit.getServer();
        server.getScheduler().runTaskAsynchronously(AlliedKingdomsBootstrapper.pluginInstance, () -> {
            try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {

                //check that the player is the owner of the kingdom
                String sql = "SELECT * FROM kingdoms WHERE name=?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, kingdom);
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    String owner = result.getString("owner");
                    if (owner.equals(player.getUniqueId().toString())) {

                        //update the throne
                        sql = "UPDATE kingdoms SET throne=? WHERE name=?";
                        PreparedStatement updateStatement = connection.prepareStatement(sql);
                        updateStatement.setString(1, throneEntry);
                        updateStatement.setString(2, kingdom);
                        updateStatement.executeUpdate();

                        //tell the player of the success
                        player.sendMessage(Component.text("Throne successfully created", Palette.PRIMARY));
                    } else {
                        player.sendMessage(Component.text("Only the kingdom's owner can designate the throne", Palette.ERROR));
                    }
                } else {
                    player.sendMessage(Component.text("Your kingdom was not found in the database. Contact the plugin developer. Check the logs.", Palette.ERROR));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }



    private static String serializeThrone(Location warp, Block gold) {
        String output = "warp=";
        output += warp.getBlockX() + "," + warp.getBlockY() + "," + warp.getBlockZ();
        output += "&throne=";
        output += gold.getX() + "," + gold.getY() + "," + gold.getZ();
        return output;
    }
}
