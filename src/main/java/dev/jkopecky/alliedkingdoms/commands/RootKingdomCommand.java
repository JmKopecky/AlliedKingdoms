package dev.jkopecky.alliedkingdoms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.jkopecky.alliedkingdoms.Palette;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class RootKingdomCommand {



    public static void register(Commands registrar) {

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("kingdom")
                .executes(RootKingdomCommand::help);

        root.then(CreateKingdomCommand.get());
        root.then(DeleteKingdomCommand.get());
        root.then(KingdomInfoCommand.get());
        root.then(KingdomInfoCommand.getNoArgument());
        root.then(ClaimLandCommand.get());
        root.then(UnclaimLandCommand.get());
        root.then(KingdomTeleportCommand.get());
        root.then(KingdomDepositCommand.get());
        root.then(KingdomWithdrawCommand.get());

        String description = "Manage kingdoms";
        ArrayList<String> aliases = new ArrayList<>();
        aliases.add("k");

        registrar.register(root.build(), description, aliases);

    }





    public static int help(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        //todo create help output
        sender.sendMessage(Component.text("Kingdoms Command List:", Palette.ACCENT));

        return Command.SINGLE_SUCCESS;
    }
}
