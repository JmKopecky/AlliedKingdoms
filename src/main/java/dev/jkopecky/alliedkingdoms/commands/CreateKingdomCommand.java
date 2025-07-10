package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
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
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.*;


public class CreateKingdomCommand {


    private static final String defaultTagline = "A default tagline";
    private static final boolean defaultPeaceState = true;


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(CreateKingdomCommand::execute));
    }



    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        if (!(executor instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can own a kingdom.", Palette.ERROR));
            return Command.SINGLE_SUCCESS;
        }

        String name;
        try {
            name = StringArgumentType.getString(context, "name");
        } catch (IllegalArgumentException e) {
            executor.sendMessage(Component.text("Name must be specified", Palette.ERROR));
            return Command.SINGLE_SUCCESS;
        }

        //do the rest of the command in a separate thread because we need to access the database
        Server server = Bukkit.getServer();
        server.getScheduler().runTaskAsynchronously(AlliedKingdomsBootstrapper.pluginInstance, () -> {
            //check if the player already is associated with a kingdom based on their metadata
            boolean playerAlreadyHasKingdom = false;
            String oldPlayerKingdom = "";
            boolean duplicateName = false;
            NamespacedKey playerKingdomKey = PDCDataKeys.getPlayerKingdomKey();
            PersistentDataContainer playerData = player.getPersistentDataContainer();
            if (playerData.has(playerKingdomKey, PersistentDataType.STRING)) {
                playerAlreadyHasKingdom = true;
                oldPlayerKingdom = playerData.get(playerKingdomKey, PersistentDataType.STRING);
            }

            try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                Statement statement = connection.createStatement();

                if (!playerAlreadyHasKingdom) {
                    //check if player is listed as owner or member of a kingdom in the database
                    //this will only set playerAlreadyHasKingdom to true in the case of a disparity between player-stored data and what's in the database.
                    //otherwise, the player simply isn't part of a kingdom, so we can continue to create the kingdom
                    String playerUUID = player.getUniqueId().toString();
                    String sql = "SELECT * FROM kingdoms";
                    ResultSet result = statement.executeQuery(sql);
                    while (result.next()) {
                        String kingdom = result.getString("name");
                        String owner = result.getString("owner");
                        String members = result.getString("members");
                        if (owner.equals(playerUUID)) {
                            playerAlreadyHasKingdom = true;
                            oldPlayerKingdom = kingdom;
                        }
                        for (String member : members.split(",")) {
                            if (member.equals(playerUUID)) {
                                playerAlreadyHasKingdom = true;
                                oldPlayerKingdom = kingdom;
                                break;
                            }
                        }
                        if (kingdom.equals(name)) {
                            duplicateName = true;
                        }
                    }
                }

                if (playerAlreadyHasKingdom) {
                    if (executor == sender) {
                        executor.sendMessage(Component.text("You are already a member of kingdom ", Palette.PRIMARY)
                                .append(Component.text(oldPlayerKingdom, Palette.ACCENT)));
                    } else {
                        sender.sendMessage(Component.text(player.getName(), Palette.ACCENT)
                                .append(Component.text(" is already a member of kingdom ", Palette.PRIMARY))
                                .append(Component.text(oldPlayerKingdom, Palette.ACCENT)));
                    }
                    return;
                }

                if (duplicateName) {
                    sender.sendMessage(Component.text("Kingdom " + name + " already exists.", Palette.ERROR));
                    return;
                }

                //create the kingdom
                String sql = "INSERT INTO kingdoms " +
                        "(name, owner, tagline, peaceful, members, chunks, throne, vault) " +
                        "VALUES (?,?,?,?,?,?,?,?)";
                PreparedStatement createKingdomStatement = connection.prepareStatement(sql);
                createKingdomStatement.setString(1, name);
                createKingdomStatement.setString(2, player.getUniqueId().toString());
                createKingdomStatement.setString(3, defaultTagline);
                createKingdomStatement.setBoolean(4, defaultPeaceState);
                String members = player.getUniqueId().toString();
                createKingdomStatement.setString(5, members);
                createKingdomStatement.setString(6, "");
                createKingdomStatement.setString(7, "");
                createKingdomStatement.setDouble(8, 0);
                createKingdomStatement.executeUpdate();

                playerData.set(playerKingdomKey, PersistentDataType.STRING, name);

                //notify user of success
                if (executor == sender) {
                    sender.sendMessage(Component.text("You have created the kingdom ", Palette.PRIMARY)
                            .append(Component.text(name, Palette.ACCENT)));
                    return;
                }

                executor.sendMessage(Component.text("You are now the owner of the kingdom ", Palette.PRIMARY)
                        .append(Component.text(name, Palette.ACCENT)));
                sender.sendMessage(Component.text(player.getName(), Palette.ACCENT)
                        .append(Component.text(" is now the owner of the kingdom ", Palette.PRIMARY)
                                .append(Component.text(name, Palette.ACCENT))));
            } catch (SQLException e) {
                System.out.println(e);
                e.printStackTrace();
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
