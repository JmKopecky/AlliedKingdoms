package dev.jkopecky.alliedkingdoms;

import dev.jkopecky.alliedkingdoms.commands.RootKingdomCommand;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class AlliedKingdomsBootstrapper implements PluginBootstrap {

    public static JavaPlugin pluginInstance = null;

    @Override
    public void bootstrap(BootstrapContext bootstrapContext) {
        bootstrapContext.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            RootKingdomCommand.register(commands.registrar());
        });
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        pluginInstance = new AlliedKingdoms();
        return pluginInstance;
    }
}
