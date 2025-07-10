package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.*;

public class KingdomTeleportCommand {


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("tp")
                .executes(KingdomTeleportCommand::execute);
    }


    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        if (executor instanceof Player player) {

            //get player kingdom
            PersistentDataContainer container = player.getPersistentDataContainer();
            String kingdom = "";
            if (container.has(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING)) {
                kingdom = container.get(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING);
            } else {
                return Command.SINGLE_SUCCESS;
            }

            //access database to find kingdom throne tp location
            try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                String sql = "SELECT * FROM kingdoms WHERE name=?;";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, kingdom);
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    String throne = result.getString("throne");
                    if (throne.isEmpty()) {
                        player.sendMessage(Component.text("Your kingdom does not have a throne", Palette.ERROR));
                        return Command.SINGLE_SUCCESS;
                    }
                    String[] warpLocation = throne.split("&throne=")[0].split("warp=")[1].split(",");
                    player.sendMessage(Component.text("Teleporting...", Palette.PRIMARY));
                    player.teleport(new Location(player.getWorld(),
                            Double.parseDouble(warpLocation[0]), Double.parseDouble(warpLocation[1]), Double.parseDouble(warpLocation[2])));
                } else {
                    throw new SQLException();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                sender.sendMessage(Component.text("Failed to access player's kingdom", Palette.ERROR));
            }

        } else {
            sender.sendMessage(Component.text("Target must be a player", Palette.ERROR));
        }

        return Command.SINGLE_SUCCESS;
    }
}
