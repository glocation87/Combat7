package io.github.glocation87.combat7;

import org.bukkit.Tag;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public final class Weapons {

    private Weapons() {
    }

    public static boolean isSword(@Nullable ItemStack item) {
        return item != null && Tag.ITEMS_SWORDS.isTagged(item.getType());
    }

    public static boolean holdsSword(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        return equipment != null && isSword(equipment.getItemInMainHand());
    }
}
