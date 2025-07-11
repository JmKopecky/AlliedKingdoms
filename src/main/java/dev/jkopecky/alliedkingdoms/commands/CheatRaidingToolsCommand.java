package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.Database;
import dev.jkopecky.alliedkingdoms.util.RaidGearGetters;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;

public class CheatRaidingToolsCommand {


    public static void register(Commands registrar) {

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("kingdomcheats")
                .then(Commands.literal("raidtools").executes(CheatRaidingToolsCommand::execute));

        String description = "Receive all raid-capable tools";

        registrar.register(root.build(), description);

    }



    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Entity executor = context.getSource().getExecutor();

        if (executor instanceof Player player) {
            player.sendMessage(Component.text("Received raiding tools", Palette.PRIMARY));
            //what items do we want to give?
            //pick, axe, shovel, some kind of placeable explosive, one form of placeable block
            //different levels of each?

            //wooden tools
            player.give(RaidGearGetters.woodPickaxe());
            player.give(RaidGearGetters.woodAxe());
            player.give(RaidGearGetters.woodShovel());
            //stone tools
            player.give(RaidGearGetters.stonePickaxe());
            player.give(RaidGearGetters.stoneAxe());
            player.give(RaidGearGetters.stoneShovel());
            //gold tools
            player.give(RaidGearGetters.goldPickaxe());
            player.give(RaidGearGetters.goldAxe());
            player.give(RaidGearGetters.goldShovel());
            //iron tools
            player.give(RaidGearGetters.ironPickaxe());
            player.give(RaidGearGetters.ironAxe());
            player.give(RaidGearGetters.ironShovel());
        } else {
            sender.sendMessage(Component.text("Target must be a player", Palette.ERROR));
        }

        return Command.SINGLE_SUCCESS;
    }
}
