package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.Message;
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

import java.sql.*;
import java.util.UUID;


public class KingdomTopCommand {


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("top")
                        .executes(KingdomTopCommand::execute);
    }



    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        Server server = Bukkit.getServer();
        server.getScheduler().runTaskAsynchronously(AlliedKingdomsBootstrapper.pluginInstance, () -> {
            try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                Statement statement = connection.createStatement();
                String sql = "SELECT * FROM kingdoms ORDER BY accruedvalue DESC LIMIT 10;";
                ResultSet result = statement.executeQuery(sql);
                TextComponent message = Component.text("Top Kingdoms:");
                while (result.next()) {
                    message = message.appendNewline()
                            .append(Component.text(result.getString("name"), Palette.PRIMARY)
                            .append(Component.text(" - ", Palette.PRIMARY)
                            .append(Component.text(result.getDouble("accruedvalue"), Palette.PRIMARY))));
                }
                if (executor != null) {
                    executor.sendMessage(message);
                } else {
                    sender.sendMessage(Component.text("Failed to execute command", Palette.ERROR));
                }
            } catch (SQLException e) {
                sender.sendMessage(Component.text("Failed to execute command", Palette.ERROR));
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
