package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import dev.jkopecky.alliedkingdoms.util.KingdomUtilMethods;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.*;


public class DeleteKingdomCommand {



    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("delete").executes(DeleteKingdomCommand::execute);
    }



    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        if (!(executor instanceof Player player)) {
            sender.sendMessage(Component.text("Players cannot delete what they cannot own.", Palette.ERROR));
            return Command.SINGLE_SUCCESS;
        }

        Server server = Bukkit.getServer();
        server.getScheduler().runTaskAsynchronously(AlliedKingdomsBootstrapper.pluginInstance, () -> {

            //check the player's metadata for an associated kingdom
            NamespacedKey playerKingdomKey = PDCDataKeys.getPlayerKingdomKey();
            PersistentDataContainer playerData = player.getPersistentDataContainer();
            String kingdomName = null;
            if (playerData.has(playerKingdomKey, PersistentDataType.STRING)) {
                kingdomName = playerData.get(playerKingdomKey, PersistentDataType.STRING);
            }

            try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                if (kingdomName == null) {
                    String playerUUID = player.getUniqueId().toString();
                    //loop through all kingdoms to see if the player owns any of them.
                    String sql = "SELECT * FROM kingdoms WHERE owner=?";
                    PreparedStatement findKingdomStatement = connection.prepareStatement(sql);
                    findKingdomStatement.setString(1, playerUUID);
                    ResultSet result = findKingdomStatement.executeQuery();
                    if (result.next()) {
                        kingdomName = result.getString("name");
                    }
                }

                if (kingdomName == null) {
                    if (executor == sender) {
                        executor.sendMessage(Component.text("You do not own a kingdom", Palette.PRIMARY));
                    } else {
                        sender.sendMessage(Component.text(player.getName(), Palette.ACCENT)
                                .append(Component.text(" does not own a kingdom", Palette.PRIMARY)));
                    }
                    return;
                }

                //unclaim all chunks
                KingdomUtilMethods.unclaimAll(kingdomName, server);

                //delete kingdom
                String deleteSql = "DELETE FROM kingdoms WHERE name=?";
                PreparedStatement preparedStatement = connection.prepareStatement(deleteSql);
                preparedStatement.setString(1, kingdomName);
                preparedStatement.executeUpdate();

                playerData.remove(playerKingdomKey);

                if (executor != sender) {
                    sender.sendMessage(Component.text(player.getName() + "'s", Palette.ACCENT)
                            .append(Component.text(" kingdom ", Palette.PRIMARY)
                            .append(Component.text(kingdomName, Palette.ACCENT)
                            .append(Component.text(" has been deleted", Palette.PRIMARY)))));
                }
                executor.sendMessage(Component.text("Your kingdom ", Palette.PRIMARY)
                        .append(Component.text(kingdomName, Palette.ACCENT)
                        .append(Component.text(" has been deleted", Palette.PRIMARY))));

            } catch (SQLException e) {
                System.out.println(e);
                e.printStackTrace();
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
