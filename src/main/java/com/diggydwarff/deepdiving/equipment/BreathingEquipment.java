package com.diggydwarff.deepdiving.equipment;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.gas.GasMix;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One selected supply owns each second of breathing; fallback never drains both tanks. */
public final class BreathingEquipment {
    public record Supply(
        ItemStack stack,
        GasMix mix,
        double remaining,
        double capacity,
        double rating,
        boolean create,
        boolean emergency
    ) {
        public boolean available() {
            return remaining > 0;
        }
    }

    public static boolean carries(Player player, net.minecraft.world.item.Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }

        return player.getOffhandItem().is(item);
    }

    public static boolean helmet(Player player) {
        return DivingHelmetItem.isWornBy(player);
    }

    /** Prefer the worn cylinder, then enabled backups, then available Create backtank air. */
    public static Supply select(Player player) {
        if (!helmet(player)) {
            return null;
        }

        boolean regulator = carries(player, DeepDiving.REGULATOR.get());

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (regulator
            && chest.getItem() instanceof CylinderItem
            && CylinderItem.supply(chest).remaining() > 0) {
            return cylinder(chest, false);
        }

        if (regulator) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof CylinderItem
                    && CylinderItem.supply(stack).backupEnabled()
                    && CylinderItem.supply(stack).remaining() > 0) {
                    return cylinder(stack, true);
                }
            }

            ItemStack offhand = player.getOffhandItem();

            if (offhand.getItem() instanceof CylinderItem
                && CylinderItem.supply(offhand).backupEnabled()
                && CylinderItem.supply(offhand).remaining() > 0) {
                return cylinder(offhand, true);
            }
        }

        var tanks = BacktankUtil.getAllWithAir(player);

        if (!tanks.isEmpty()) {
            ItemStack tank = tanks.getFirst();

            return new Supply(
                tank,
                GasMix.AIR,
                BacktankUtil.getAir(tank),
                BacktankUtil.maxAir(tank),
                tank.has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT)
                        ? DivingConfig.NETHERITE_RATING.get()
                        : DivingConfig.COPPER_RATING.get(),
                true,
                false
            );
        }

        return null;
    }

    private static Supply cylinder(ItemStack stack, boolean emergency) {
        CylinderItem item = (CylinderItem) stack.getItem();

        var supply = CylinderItem.supply(stack);

        return new Supply(
            stack,
            supply.mix(),
            supply.remaining(),
            item.capacity,
            item.rating(),
            false,
            emergency || item.emergency
        );
    }

    public static double consumption(Supply supply, double pressure) {
        double scale = DivingConfig.PRESET.get() == DivingConfig.Difficulty.ARCADE ? 0.65 : 1;

        return pressure * scale * (supply.create ? 1 : DivingConfig.GAS_PER_SECOND.get());
    }

    public static double remainingSeconds(Supply supply, double pressure) {
        if (supply == null || supply.create() && !DivingConfig.CREATE_AIR.get()) {
            return -1;
        }

        return com.diggydwarff.deepdiving.physiology.DiveComputerTools.gasSeconds(
            supply.remaining(),
            consumption(supply, pressure)
        );
    }

    /**
     * Protection belongs to worn equipment; a spare cylinder cannot protect an unequipped diver.
     */
    public static double protectionRating(Player player) {
        if (!helmet(player)) {
            return DivingConfig.UNPROTECTED_RATING.get();
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chest.getItem() instanceof CylinderItem cylinder) {
            return cylinder.rating();
        }

        if (chest.getItem() instanceof com.simibubi.create.content.equipment.armor.BacktankItem) {
            return chest.has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT)
                    ? DivingConfig.NETHERITE_RATING.get()
                    : DivingConfig.COPPER_RATING.get();
        }

        return DivingConfig.UNPROTECTED_RATING.get();
    }

    public static void consume(Player player, Supply supply, double amount) {
        if (supply.create) {
            // Preserve fractional pressure drain between seconds instead of rounding every breath
            // up.
            var tag = player.getPersistentData();

            double owed = tag.getDouble("deepdiving.createAirRemainder") + amount;

            int whole = (int) Math.floor(owed);

            tag.putDouble("deepdiving.createAirRemainder", owed - whole);

            if (whole > 0) {
                BacktankUtil.consumeAir(player, supply.stack, Math.min(whole, (int) supply.remaining));
            }
        } else {
            supply.stack.set(DeepDiving.SUPPLY.get(), CylinderItem.supply(supply.stack).consume(amount));
        }
    }

    private BreathingEquipment() {}
}
