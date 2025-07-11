package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.inventories.QuartermasterInventory;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.sql.*;

public class KingdomQuartermasterCommand {


    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("quartermaster")
                .executes(KingdomQuartermasterCommand::execute);
    }



    public static int execute(CommandContext<CommandSourceStack> context) {
        Entity executor = context.getSource().getExecutor();
        CommandSender sender = context.getSource().getSender();

        if (executor instanceof Player player) {
            QuartermasterInventory inventory = new QuartermasterInventory(AlliedKingdomsBootstrapper.pluginInstance);
            player.openInventory(inventory.getInventory());
        } else {
            sender.sendMessage(Component.text("Target must be a player", Palette.ERROR));
        }

        return Command.SINGLE_SUCCESS;
    }
}
