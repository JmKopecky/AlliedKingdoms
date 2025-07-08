package dev.jkopecky.alliedkingdoms.events;

import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ClaimInterferenceListeners implements Listener {


    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        PersistentDataContainer chunkContainer = chunk.getPersistentDataContainer();
        NamespacedKey chunkKey = PDCDataKeys.getChunkKingdomKey();

        //stop early if the block isn't placed in an owned chunk
        if (!chunkContainer.has(chunkKey, PersistentDataType.STRING)) {
            return;
        }
        String chunkKingdom = chunkContainer.get(chunkKey, PersistentDataType.STRING);

        //get player kingdom
        Player player = event.getPlayer();
        PersistentDataContainer playerContainer = player.getPersistentDataContainer();
        NamespacedKey playerKey = PDCDataKeys.getPlayerKingdomKey();
        String playerKingdom = "";
        if (playerContainer.has(playerKey, PersistentDataType.STRING)) {
            playerKingdom = playerContainer.get(playerKey, PersistentDataType.STRING);
        }

        if (playerKingdom.equals(chunkKingdom)) {
            return;
        }

        //player is not allowed to place blocks in this chunk
        player.sendMessage(Component.text("You are not allowed to build in chunks owned by " + chunkKingdom, Palette.ERROR));
        event.setCancelled(true);
    }



    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        PersistentDataContainer chunkContainer = chunk.getPersistentDataContainer();
        NamespacedKey chunkKey = PDCDataKeys.getChunkKingdomKey();

        //stop early if the block isn't broken in an owned chunk
        if (!chunkContainer.has(chunkKey, PersistentDataType.STRING)) {
            return;
        }
        String chunkKingdom = chunkContainer.get(chunkKey, PersistentDataType.STRING);

        //get player kingdom
        Player player = event.getPlayer();
        PersistentDataContainer playerContainer = player.getPersistentDataContainer();
        NamespacedKey playerKey = PDCDataKeys.getPlayerKingdomKey();
        String playerKingdom = "";
        if (playerContainer.has(playerKey, PersistentDataType.STRING)) {
            playerKingdom = playerContainer.get(playerKey, PersistentDataType.STRING);
        }

        if (playerKingdom.equals(chunkKingdom)) {
            return;
        }

        //player is not allowed to break blocks in this chunk
        player.sendMessage(Component.text("You are not allowed to break blocks in chunks owned by " + chunkKingdom, Palette.ERROR));
        event.setCancelled(true);
    }
}
