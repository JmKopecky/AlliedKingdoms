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
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.units.qual.C;

import java.sql.*;
import java.util.UUID;


public class KingdomInfoCommand {


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("info")
                .then(Commands.argument("kingdom", StringArgumentType.word())
                        .executes(KingdomInfoCommand::execute));
    }


    public static LiteralArgumentBuilder<CommandSourceStack> getNoArgument() {
        return Commands.literal("info")
                .executes(KingdomInfoCommand::execute);
    }



    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        String kingdomName;
        try {
            kingdomName = StringArgumentType.getString(context, "kingdom");
        } catch (IllegalArgumentException e) {
            //no kingdom name specified, use executor's kingdom
            if (executor instanceof Player player) {
                NamespacedKey playerKingdomKey = PDCDataKeys.getPlayerKingdomKey();
                PersistentDataContainer playerData = player.getPersistentDataContainer();
                if (playerData.has(playerKingdomKey, PersistentDataType.STRING)) {
                    kingdomName = playerData.get(playerKingdomKey, PersistentDataType.STRING);
                } else {
                    sender.sendMessage(Component.text("You are not part of a kingdom", Palette.ERROR));
                    return Command.SINGLE_SUCCESS;
                }
            } else {
                sender.sendMessage(Component.text("Non-players cannot be part of kingdoms", Palette.ERROR));
                return Command.SINGLE_SUCCESS;
            }
        }

        String targetKingdom = kingdomName;
        //do the rest of the command in a separate thread because we need to access the database
        Server server = Bukkit.getServer();
        server.getScheduler().runTaskAsynchronously(AlliedKingdomsBootstrapper.pluginInstance, () -> {
            //retrieve the kingdom
            try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                String sql = "SELECT * FROM kingdoms WHERE name=?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, targetKingdom);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String tagline = resultSet.getString("tagline");
                    boolean peaceful = resultSet.getBoolean("peaceful");
                    String members = resultSet.getString("members");
                    int memberCount = members.split(",").length;
                    String ownerUUID = resultSet.getString("owner");
                    Player owner = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                    String ownerName;
                    if (owner != null) {
                        ownerName = owner.getName();
                    } else {
                        //player offline. Check database to get uuid;
                        String playerSql = "SELECT * FROM players WHERE uuid=?";
                        PreparedStatement playerStatement = connection.prepareStatement(playerSql);
                        ResultSet pResultSet = playerStatement.executeQuery();
                        if (pResultSet.next()) {
                            ownerName = pResultSet.getString("name");
                        } else {
                            ownerName = ownerUUID;
                        }
                    }

                    //print out the information
                    TextComponent message = Component.text("--- " + name + " ---", Palette.PRIMARY)
                            .appendNewline()
                            .append(Component.text("Owner: ", Palette.PRIMARY)
                            .append(Component.text(ownerName, Palette.SECONDARY)
                            .appendNewline()
                            .append(Component.text("Tagline: ", Palette.PRIMARY)
                            .append(Component.text(tagline, Palette.SECONDARY)))));
                    message = message.appendNewline();
                    if (peaceful) {
                        message = message.append(Component.text("Peaceful", Palette.ACCENT));
                    } else {
                        message = message.append(Component.text("Hostile", Palette.ERROR));
                    }
                    message = message.appendNewline()
                            .append(Component.text("Members: ", Palette.PRIMARY)
                            .append(Component.text(memberCount, Palette.ACCENT)));
                    if (executor instanceof Player player) {
                        player.sendMessage(message);
                    } else {
                        sender.sendMessage(message);
                    }
                    return;
                } else {
                    //no matching kingdom
                    sender.sendMessage(Component.text("Kingdom ", Palette.PRIMARY)
                            .append(Component.text(targetKingdom, Palette.ACCENT)
                            .append(Component.text(" does not exist", Palette.PRIMARY))));
                    return;
                }
            } catch (SQLException e) {
                System.out.println(e);
                e.printStackTrace();
            }
            sender.sendMessage(Component.text("Kingdom " + targetKingdom + " does not exist", Palette.ERROR));
        });
        return Command.SINGLE_SUCCESS;
    }
}
