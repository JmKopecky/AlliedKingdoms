package dev.jkopecky.alliedkingdoms.inventories;

import dev.jkopecky.alliedkingdoms.AlliedKingdoms;
import dev.jkopecky.alliedkingdoms.AlliedKingdomsBootstrapper;
import dev.jkopecky.alliedkingdoms.Palette;
import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import dev.jkopecky.alliedkingdoms.util.RaidGearGetters;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class QuartermasterInventory implements InventoryHolder, Listener {

    private Inventory inventory;

    //for events
    public QuartermasterInventory() {}

    public QuartermasterInventory(JavaPlugin plugin) {
        this.inventory = plugin.getServer().createInventory(this, 27, Component.text("Quartermaster", Palette.ACCENT));

        //add items
        inventory.setItem(1, RaidGearGetters.woodPickaxe());
        inventory.setItem(10, RaidGearGetters.woodAxe());
        inventory.setItem(19, RaidGearGetters.woodShovel());

        inventory.setItem(3, RaidGearGetters.stonePickaxe());
        inventory.setItem(12, RaidGearGetters.stoneAxe());
        inventory.setItem(21, RaidGearGetters.stoneShovel());

        inventory.setItem(5, RaidGearGetters.goldPickaxe());
        inventory.setItem(14, RaidGearGetters.goldAxe());
        inventory.setItem(23, RaidGearGetters.goldShovel());

        inventory.setItem(7, RaidGearGetters.ironPickaxe());
        inventory.setItem(16, RaidGearGetters.ironAxe());
        inventory.setItem(25, RaidGearGetters.ironShovel());
        //fill all unfilled slots
        for (int i = 0; i < 27; i++) {
            if (inventory.getItem(i) == null) {
                ItemStack filler = ItemStack.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = filler.getItemMeta();
                meta.setHideTooltip(true);
                filler.setItemMeta(meta);
                inventory.setItem(i, filler);
            }
        }
    }


    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inventory = e.getClickedInventory();
        // Add a null check in case the player clicked outside the window.
        if (inventory == null || !(inventory.getHolder(false) instanceof QuartermasterInventory)) {
            return;
        }

        e.setCancelled(true);

        if (e.getCurrentItem() == null) {return;}

        //get gear costs
        FileConfiguration config = AlliedKingdomsBootstrapper.pluginInstance.getConfig();
        int woodRaidToolCost = config.getInt("gear.raid-tool-wood-cost");
        int stoneRaidToolCost = config.getInt("gear.raid-tool-stone-cost");
        int goldRaidToolCost = config.getInt("gear.raid-tool-gold-cost");
        int ironRaidToolCost = config.getInt("gear.raid-tool-iron-cost");

        ItemStack clickedItem = e.getCurrentItem();
        PersistentDataContainerView container = clickedItem.getPersistentDataContainer();
        NamespacedKey key = PDCDataKeys.getRaidItemKey();
        if (!container.has(key, PersistentDataType.STRING)) {
            return;
        }

        String raidToolValue = container.get(key, PersistentDataType.STRING);

        switch (raidToolValue) {
            case "raiditem_woodpick" -> purchaseItem(woodRaidToolCost, e.getWhoClicked(), RaidGearGetters.woodPickaxe());
            case "raiditem_woodaxe" -> purchaseItem(woodRaidToolCost, e.getWhoClicked(), RaidGearGetters.woodAxe());
            case "raiditem_woodshovel" -> purchaseItem(woodRaidToolCost, e.getWhoClicked(), RaidGearGetters.woodShovel());
            case "raiditem_stonepick" -> purchaseItem(stoneRaidToolCost, e.getWhoClicked(), RaidGearGetters.stonePickaxe());
            case "raiditem_stoneaxe" -> purchaseItem(stoneRaidToolCost, e.getWhoClicked(), RaidGearGetters.stoneAxe());
            case "raiditem_stoneshovel" -> purchaseItem(stoneRaidToolCost, e.getWhoClicked(), RaidGearGetters.stoneShovel());
            case "raiditem_goldpick" -> purchaseItem(goldRaidToolCost, e.getWhoClicked(), RaidGearGetters.goldPickaxe());
            case "raiditem_goldaxe" -> purchaseItem(goldRaidToolCost, e.getWhoClicked(), RaidGearGetters.goldAxe());
            case "raiditem_goldshovel" -> purchaseItem(goldRaidToolCost, e.getWhoClicked(), RaidGearGetters.goldShovel());
            case "raiditem_ironpick" -> purchaseItem(ironRaidToolCost, e.getWhoClicked(), RaidGearGetters.ironPickaxe());
            case "raiditem_ironaxe" -> purchaseItem(ironRaidToolCost, e.getWhoClicked(), RaidGearGetters.ironAxe());
            case "raiditem_ironshovel" -> purchaseItem(ironRaidToolCost, e.getWhoClicked(), RaidGearGetters.ironShovel());
        }
    }



    private static void purchaseItem(int cost, HumanEntity entity, ItemStack item) {
        if (entity instanceof Player player) {
            Economy economy = AlliedKingdoms.ECONOMY;
            if (economy.has(player, cost)) {
                economy.withdrawPlayer(player, cost);
                player.give(item);
                player.sendMessage(Component.text("Purchased ", Palette.PRIMARY)
                        .append(item.getItemMeta().customName()
                        .append(Component.text(" for " + cost, Palette.PRIMARY))));
            } else {
                //insufficient funds
                player.sendMessage(Component.text("You do not have sufficient funds for this purchase", Palette.PRIMARY));
            }
        }
    }
}
