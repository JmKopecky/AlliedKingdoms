package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.jkopecky.alliedkingdoms.AlliedKingdoms;
import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.*;

public class KingdomDepositCommand {


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("deposit")
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                        .executes(KingdomDepositCommand::execute));
    }


    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        if (executor instanceof Player player) {

            double depositAmount;
            try {
                depositAmount = DoubleArgumentType.getDouble(context, "amount");
            } catch (IllegalArgumentException e) {
                executor.sendMessage(Component.text("Amount must be specified", Palette.ERROR));
                return Command.SINGLE_SUCCESS;
            }

            //get player kingdom
            PersistentDataContainer container = player.getPersistentDataContainer();
            String kingdom = "";
            if (container.has(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING)) {
                kingdom = container.get(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING);
            } else {
                player.sendMessage(Component.text("You must be part of a kingdom to deposit", Palette.ERROR));
                return Command.SINGLE_SUCCESS;
            }

            //access database
            Server server = Bukkit.getServer();
            String kingdomName = kingdom;
            server.getScheduler().runTaskAsynchronously(AlliedKingdomsBootstrapper.pluginInstance, () -> {
                try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                    String sql = "SELECT * FROM kingdoms WHERE name=?;";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, kingdomName);
                    ResultSet result = statement.executeQuery();
                    if (result.next()) {
                        //check if sufficient funds
                        if (AlliedKingdoms.ECONOMY.has(player, depositAmount)) {
                            //withdraw from player then add to kingdom vault
                            AlliedKingdoms.ECONOMY.withdrawPlayer(player, depositAmount);
                            sql = "UPDATE kingdoms set vault=vault+? WHERE name=?;";
                            statement = connection.prepareStatement(sql);
                            statement.setDouble(1, depositAmount);
                            statement.setString(2, kingdomName);
                            statement.executeUpdate();
                            player.sendMessage(Component.text("Successfully deposited ", Palette.PRIMARY)
                                    .append(Component.text(depositAmount, Palette.ACCENT)
                                    .append(Component.text(" to your kingdom", Palette.PRIMARY))));
                        } else {
                            player.sendMessage(Component.text("You do not have sufficient funds to deposit the specified amount", Palette.ERROR));
                        }
                    } else {
                        throw new SQLException();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    sender.sendMessage(Component.text("Failed to access player's kingdom", Palette.ERROR));
                }
            });
        } else {
            sender.sendMessage(Component.text("Target must be a player", Palette.ERROR));
        }
        return Command.SINGLE_SUCCESS;
    }
}
