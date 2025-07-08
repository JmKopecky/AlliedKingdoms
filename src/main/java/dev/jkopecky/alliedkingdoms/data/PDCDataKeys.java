package dev.jkopecky.alliedkingdoms.data;

import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import org.bukkit.NamespacedKey;

public class PDCDataKeys {



    private static NamespacedKey playerKingdomKey = null;
    private static NamespacedKey chunkKingdomKey = null;


    public static NamespacedKey getPlayerKingdomKey() {
        if (playerKingdomKey == null) {
            playerKingdomKey = new NamespacedKey(AlliedKingdomsBootstrapper.pluginInstance, "player_kingdom");
        }
        return playerKingdomKey;
    }


    public static NamespacedKey getChunkKingdomKey() {
        if (chunkKingdomKey == null) {
            chunkKingdomKey = new NamespacedKey(AlliedKingdomsBootstrapper.pluginInstance, "chunk_kingdom");
        }
        return chunkKingdomKey;
    }
}
