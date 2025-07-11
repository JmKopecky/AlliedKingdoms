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
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.units.qual.C;

import java.sql.*;


public class KingdomTaglineCommand {


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("tagline")
                .then(Commands.argument("tagline", StringArgumentType.greedyString())
                        .executes(KingdomTaglineCommand::execute));
    }



    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        if (!(executor instanceof Player player)) {
            sender.sendMessage(Component.text("Target must be a player", Palette.ERROR));
            return Command.SINGLE_SUCCESS;
        }

        //get player kingdom
        PersistentDataContainer container = player.getPersistentDataContainer();
        String kingdom = "";
        if (container.has(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING)) {
            kingdom = container.get(PDCDataKeys.getPlayerKingdomKey(), PersistentDataType.STRING);
        } else {
            if (executor == sender) {
                executor.sendMessage(Component.text("You must be part of a kingdom", Palette.ERROR));
            } else {
                sender.sendMessage(Component.text("Target must be part of a kingdom", Palette.ERROR));
            }
            return Command.SINGLE_SUCCESS;
        }

        String tagline;
        try {
            tagline = StringArgumentType.getString(context, "tagline");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Tagline must be specified", Palette.ERROR));
            return Command.SINGLE_SUCCESS;
        }

        //check tagline max length
        int maxTaglineLength = AlliedKingdomsBootstrapper.pluginInstance.getConfig().getInt("metadata.max-tagline-length");
        if (tagline.length() > maxTaglineLength) {
            sender.sendMessage(Component.text("Tagline is too long", Palette.ERROR));
            return Command.SINGLE_SUCCESS;
        }

        //update kingdom tagline
        String kingdomForLambda = kingdom;
        Server server = Bukkit.getServer();
        server.getScheduler().runTaskAsynchronously(AlliedKingdomsBootstrapper.pluginInstance, () -> {
            try (Connection connection = DriverManager.getConnection(Database.databaseUrl)) {
                String sql = "UPDATE kingdoms SET tagline=? WHERE name=?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, tagline);
                statement.setString(2, kingdomForLambda);
                statement.executeUpdate();
            } catch (SQLException e) {
                sender.sendMessage(Component.text("Failed to execute command", Palette.ERROR));
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
