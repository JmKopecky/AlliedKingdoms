package dev.jkopecky.alliedkingdoms.util;

import dev.jkopecky.alliedkingdoms.data.PDCDataKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@SuppressWarnings("UnstableApiUsage")
public class RaidGearGetters {


    public static ArrayList<TextComponent> getLore() {
        ArrayList<TextComponent> lore = new ArrayList<>();
        lore.add(Component.text("This item can be used to raid hostile kingdoms."));
        lore.add(Component.text("Peaceful kingdoms remain protected."));
        return lore;
    }


    private static ItemStack getItemStack(ItemStack tool, ItemMeta meta) {
        meta.setEnchantmentGlintOverride(true);
        meta.setEnchantable(1);
        tool.setItemMeta(meta);
        tool.lore(getLore());
        return tool;
    }


    //wooden tools
    public static ItemStack woodPickaxe() {
        ItemStack tool = ItemStack.of(Material.WOODEN_PICKAXE);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_woodpick");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_PICKAXE, 1.2f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Wood Pickaxe"));
        return getItemStack(tool, meta);
    }

    public static ItemStack woodAxe() {
        ItemStack tool = ItemStack.of(Material.WOODEN_AXE);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_woodaxe");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_AXE, 1.2f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Wood Axe"));
        return getItemStack(tool, meta);
    }

    public static ItemStack woodShovel() {
        ItemStack tool = ItemStack.of(Material.WOODEN_SHOVEL);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_woodshovel");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_SHOVEL, 1.2f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Wood Shovel"));
        return getItemStack(tool, meta);
    }


    //stone tools
    public static ItemStack stonePickaxe() {
        ItemStack tool = ItemStack.of(Material.STONE_PICKAXE);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_stonepick");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_PICKAXE, 2f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Stone Pickaxe"));
        return getItemStack(tool, meta);
    }

    public static ItemStack stoneAxe() {
        ItemStack tool = ItemStack.of(Material.STONE_AXE);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_stoneaxe");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_AXE, 2f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Stone Axe"));
        return getItemStack(tool, meta);
    }

    public static ItemStack stoneShovel() {
        ItemStack tool = ItemStack.of(Material.STONE_SHOVEL);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_stoneshovel");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_SHOVEL, 2f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Stone Shovel"));
        return getItemStack(tool, meta);
    }


    //gold tools
    public static ItemStack goldPickaxe() {
        ItemStack tool = ItemStack.of(Material.GOLDEN_PICKAXE);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_goldpick");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_PICKAXE, 4f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(2);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Gold Pickaxe"));
        return getItemStack(tool, meta);
    }

    public static ItemStack goldAxe() {
        ItemStack tool = ItemStack.of(Material.GOLDEN_AXE);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_goldaxe");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_AXE, 4f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(2);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Gold Axe"));
        return getItemStack(tool, meta);
    }

    public static ItemStack goldShovel() {
        ItemStack tool = ItemStack.of(Material.GOLDEN_SHOVEL);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_goldshovel");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_SHOVEL, 4f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(2);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Gold Shovel"));
        return getItemStack(tool, meta);
    }


    //iron tools
    public static ItemStack ironPickaxe() {
        ItemStack tool = ItemStack.of(Material.IRON_PICKAXE);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_ironpick");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_PICKAXE, 3f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Iron Pickaxe"));
        return getItemStack(tool, meta);
    }

    public static ItemStack ironAxe() {
        ItemStack tool = ItemStack.of(Material.IRON_AXE);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_ironaxe");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_AXE, 3f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Iron Axe"));
        return getItemStack(tool, meta);
    }

    public static ItemStack ironShovel() {
        ItemStack tool = ItemStack.of(Material.IRON_SHOVEL);
        ItemMeta meta = tool.getItemMeta();
        meta.getPersistentDataContainer().set(PDCDataKeys.getRaidItemKey(), PersistentDataType.STRING, "raiditem_ironshovel");
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_SHOVEL, 3f, true);
        toolComponent.setDefaultMiningSpeed(0.5f);
        toolComponent.setDamagePerBlock(5);
        meta.setTool(toolComponent);
        meta.customName(Component.text("Raider's Iron Shovel"));
        return getItemStack(tool, meta);
    }
}
