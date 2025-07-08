package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.*;

public class UnclaimLandCommand {


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("unclaim")
                .executes(UnclaimLandCommand::execute);
    }


    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        if (executor instanceof Player player) {

            Chunk chunk = player.getChunk();

            //check if chunk is available to claim
            NamespacedKey chunkKingdomKey = PDCDataKeys.getChunkKingdomKey();
            PersistentDataContainer chunkContainer = chunk.getPersistentDataContainer();
            if (!chunkContainer.has(chunkKingdomKey, PersistentDataType.STRING)) {
                sender.sendMessage(Component.text("No kingdoms own this chunk", Palette.ERROR));
                return Command.SINGLE_SUCCESS;
            }
            String existingKingdomName = chunkContainer.get(chunkKingdomKey, PersistentDataType.STRING);

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

            //check if player is of the same kingdom as the chunk
            if (playerKingdom.equals(existingKingdomName)) {
                //unclaim the chunk
                chunkContainer.remove(chunkKingdomKey);
                //remove from kingdom database
                try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                    String sql = "SELECT * FROM kingdoms WHERE name=?";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, playerKingdom);
                    ResultSet result = statement.executeQuery();

                    String chunks = "";
                    if (result.next()) {
                        chunks = result.getString("chunks");
                    } else {
                        throw new SQLException();
                    }
                    chunks = chunks.replace("" + chunk.getChunkKey(), "");
                    chunks = chunks.replaceAll(",,", ",");
                    if (chunks.equals(",")) {
                        chunks = "";
                    } else {
                        if (chunks.startsWith(",")) {
                            chunks = chunks.substring(1);
                        }
                        if (chunks.endsWith(",")) {
                            chunks = chunks.substring(0, chunks.length() - 1);
                        }
                    }
                    sql = "UPDATE kingdoms SET chunks=? WHERE name=?;";
                    statement = connection.prepareStatement(sql);
                    statement.setString(1, chunks);
                    statement.setString(2, playerKingdom);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                    chunkContainer.remove(chunkKingdomKey);
                    sender.sendMessage(Component.text("Failed to unclaim chunk - database error. Tell the developer that they're stupid and need to look at the logs", Palette.ERROR));
                }

                executor.sendMessage(Component.text("Your kingdom has unclaimed this chunk", Palette.PRIMARY));
                if (executor != sender) {
                    sender.sendMessage(Component.text(player.getName(), Palette.ACCENT)
                            .append(Component.text(" has unclaimed a chunk for ", Palette.PRIMARY)
                                    .append(Component.text(playerKingdom, Palette.ACCENT))));
                }
            } else {
                if (executor == sender) {
                    executor.sendMessage(Component.text("You do not have permission to unclaim chunks from other kingdoms", Palette.ERROR));
                } else {
                    sender.sendMessage(Component.text(player.getName() + " does not have permission to unclaim chunks from other kingdoms", Palette.ERROR));
                }
            }
        } else {
            sender.sendMessage(Component.text("Only players can unclaim land", Palette.ERROR));
        }
        return Command.SINGLE_SUCCESS;
    }
}
