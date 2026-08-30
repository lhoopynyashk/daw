package dev.lhoopy.slime;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import ru.cristalix.core.model.BedrockModelParameters;
import gg.cristalix.wada.common.item.util.NbtUtil;

import java.util.Arrays;

public final class SlimeVacuumItem {
    private static final String DISPLAY_NAME = ChatColor.LIGHT_PURPLE + "Сосалка слаймов";
    private static final String MARKER = ChatColor.DARK_GRAY + "slimes:item:vacuum:v1";
    private static final String LEGACY_MARKER = ChatColor.DARK_GRAY + "slimes:sosalka";
    private static final String VACPACK_MODEL_ID = "ffffffff-ffff-ffff-a148-000000000001";

    private SlimeVacuumItem() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(DISPLAY_NAME);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "ПКМ рядом с заинтересованным слаймом",
                ChatColor.GRAY + "или прямо по нему, чтобы начать поимку.",
                ChatColor.GRAY + "ПКМ без цели: собрать плорты в вакпак.",
                ChatColor.GRAY + "Shift+ПКМ: продать плорты из вакпака.",
                ChatColor.DARK_PURPLE + "Вакпак юного ранчера",
                MARKER
        ));
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        NbtUtil.setNBT(item, BedrockModelParameters.MODEL_PATH_V2, "slimehunt:models/vacpack.model");
        NbtUtil.setNBT(item, BedrockModelParameters.TEXTURE, "texture");
        NbtUtil.setNBT(item, "p13nModelId", VACPACK_MODEL_ID);
        NbtUtil.setNBT(item, "slimesItem", "vacuum");
        return item;
    }

    public static boolean isVacuum(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName()
                && DISPLAY_NAME.equals(meta.getDisplayName())
                && meta.hasLore()
                && (meta.getLore().contains(MARKER) || meta.getLore().contains(LEGACY_MARKER));
    }

    public static boolean ensureInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isVacuum(item)) {
                return false;
            }
        }
        player.getInventory().addItem(create());
        return true;
    }
}
