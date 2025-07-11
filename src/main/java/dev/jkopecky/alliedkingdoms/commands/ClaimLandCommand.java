package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.xml.transform.Result;
import java.sql.*;
import java.util.ArrayList;

public class ClaimLandCommand {


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("claim")
                .executes(ClaimLandCommand::execute);
    }


    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        if (executor instanceof Player player) {

            Chunk chunk = player.getChunk();

            //check if chunk is in the overworld
            if (!chunk.getWorld().getName().equals(player.getServer().getWorlds().getFirst().getName())) {
                sender.sendMessage(Component.text("Chunk must be in the overworld", Palette.ERROR));
            }

            //check if chunk is available to claim
            NamespacedKey chunkKingdomKey = PDCDataKeys.getChunkKingdomKey();
            PersistentDataContainer chunkContainer = chunk.getPersistentDataContainer();
            if (chunkContainer.has(chunkKingdomKey, PersistentDataType.STRING)) {
                String existingKingdomName = chunkContainer.get(chunkKingdomKey, PersistentDataType.STRING);
                sender.sendMessage(Component.text("This chunk is already claimed by ", Palette.PRIMARY)
                        .append(Component.text(existingKingdomName, Palette.ACCENT)));
                return Command.SINGLE_SUCCESS;
            }

            //check if player is part of a kingdom
            NamespacedKey playerKingdomKey = PDCDataKeys.getPlayerKingdomKey();
            PersistentDataContainer playerContainer = player.getPersistentDataContainer();
            String playerKingdom;
            if (playerContainer.has(playerKingdomKey, PersistentDataType.STRING)) {
                playerKingdom = playerContainer.get(playerKingdomKey, PersistentDataType.STRING);
            } else {
                if (executor == sender) {
                    sender.sendMessage(Component.text("You are not part of a kingdom.", Palette.ERROR));
                } else {
                    sender.sendMessage(Component.text(player.getName(), Palette.ACCENT)
                            .append(Component.text(" is not part of a kingdom", Palette.PRIMARY)));
                }
                return Command.SINGLE_SUCCESS;
            }

            Server server = Bukkit.getServer();
            server.getScheduler().runTaskAsynchronously(AlliedKingdomsBootstrapper.pluginInstance, () -> {
                try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                    String sql = "SELECT * FROM kingdoms WHERE name=?";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, playerKingdom);
                    ResultSet result = statement.executeQuery();

                    //get prior chunk list
                    String chunks = "";
                    if (result.next()) {
                        chunks = result.getString("chunks");
                    } else {
                        throw new SQLException();
                    }

                    //check adjacent chunks
                    boolean forceAdjacent = AlliedKingdomsBootstrapper.pluginInstance.getConfig().getBoolean("claim.force-adjacent");
                    if (!chunks.isEmpty() && forceAdjacent) {
                        int chunkX = chunk.getX();
                        int chunkZ = chunk.getZ();
                        boolean adjacent = false;

                        //get all adjacent chunks
                        ArrayList<Chunk> adjacentChunks = new ArrayList<>();
                        adjacentChunks.add(chunk.getWorld().getChunkAt(chunkX + 1, chunkZ));
                        adjacentChunks.add(chunk.getWorld().getChunkAt(chunkX - 1, chunkZ));
                        adjacentChunks.add(chunk.getWorld().getChunkAt(chunkX, chunkZ + 1));
                        adjacentChunks.add(chunk.getWorld().getChunkAt(chunkX, chunkZ - 1));

                        //check if kingdom matches
                        for (Chunk adjChunk : adjacentChunks) {
                            PersistentDataContainer adjChunkContainer = adjChunk.getPersistentDataContainer();
                            if (adjChunkContainer.has(chunkKingdomKey, PersistentDataType.STRING)) {
                                String kName = adjChunkContainer.get(chunkKingdomKey, PersistentDataType.STRING);
                                if (kName.equals(playerKingdom)) {
                                    adjacent = true;
                                    break;
                                }
                            }
                        }
                        if (!adjacent) {
                            if (sender == executor) {
                                executor.sendMessage(Component.text("Chunks claimed after the first must be adjacent to other claimed territory", Palette.ERROR));
                            } else {
                                sender.sendMessage(Component.text("Chunks claimed after the first must be adjacent to other claimed territory", Palette.ERROR));
                            }
                            return;
                        }
                    }

                    //check max chunk limit
                    int maxChunks = AlliedKingdomsBootstrapper.pluginInstance.getConfig().getInt("claim.max-claims");
                    if (chunks.split(",").length >= maxChunks) {
                        if (executor == sender) {
                            executor.sendMessage(Component.text("Your kingdom already has the maximum claims", Palette.ERROR));
                        } else {
                            sender.sendMessage(Component.text("Target's kingdom already has the maximum claims", Palette.ERROR));
                        }
                        return;
                    }

                    //claim chunk
                    chunkContainer.set(chunkKingdomKey, PersistentDataType.STRING, playerKingdom);

                    //add chunk to list
                    if (chunks.isEmpty()) {
                        chunks += chunk.getChunkKey();
                    } else {
                        chunks += "," + chunk.getChunkKey();
                    }

                    //update database
                    sql = "UPDATE kingdoms SET chunks=? WHERE name=?;";
                    statement = connection.prepareStatement(sql);
                    statement.setString(1, chunks);
                    statement.setString(2, playerKingdom);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                    chunkContainer.remove(chunkKingdomKey);
                    sender.sendMessage(Component.text("Failed to claim chunk - database error. Tell the developer that they're stupid and need to look at the logs", Palette.ERROR));
                    return;
                }
                executor.sendMessage(Component.text("Your kingdom has claimed this chunk", Palette.PRIMARY));
                if (executor != sender) {
                    sender.sendMessage(Component.text(player.getName(), Palette.ACCENT)
                            .append(Component.text(" has claimed a chunk for ", Palette.PRIMARY)
                                    .append(Component.text(playerKingdom, Palette.ACCENT))));
                }
            });
        } else {
            sender.sendMessage(Component.text("Land claimer must be a player", Palette.ERROR));
        }

        return Command.SINGLE_SUCCESS;
    }
}
