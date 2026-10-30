package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;
import com.diggydwarff.deepdiving.equipment.CylinderItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Progression teaches preparation and backup use without rewarding dangerous exposure. */
public final class DivingAdvancements {
    public static void check(
            ServerPlayer player, com.diggydwarff.deepdiving.physiology.DiverState state) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chest.getItem() instanceof CylinderItem
                || player.getOffhandItem().getItem() instanceof CylinderItem
                || player.getInventory().items.stream()
                        .anyMatch(stack -> stack.getItem() instanceof CylinderItem)) {
            award(player, "root");
        }

        boolean equipped =
                BreathingEquipment.helmet(player)
                        && chest.getItem() instanceof CylinderItem
                        && CylinderItem.supply(chest).remaining() > 0
                        && BreathingEquipment.carries(player, DeepDiving.COMPUTER.get());

        if (equipped) {
            award(player, "ready_to_dive");

            if (state.underwater
                    && state.supplied
                    && state.oxygenation >= 70
                    && state.pressure <= BreathingEquipment.protectionRating(player)
                    && (state.warnings & (DiverService.HYPOXIA | DiverService.TOXICITY)) == 0) {
                award(player, "first_dive");
            }
        }

        if (player.getInventory().items.stream().anyMatch(DivingAdvancements::backup)
                || backup(player.getOffhandItem())) {
            award(player, "backup_ready");
        }
    }

    private static boolean backup(ItemStack stack) {
        return stack.getItem() instanceof CylinderItem
                && CylinderItem.supply(stack).backupEnabled()
                && CylinderItem.supply(stack).remaining() > 0;
    }

    private static void award(ServerPlayer player, String name) {
        var holder = player.server.getAdvancements().get(DeepDiving.id("diving/" + name));

        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    private DivingAdvancements() {}
}
