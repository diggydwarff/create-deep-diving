package com.diggydwarff.deepdiving.equipment;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.gas.CylinderSupply;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class CylinderItem extends BlockItem implements Equipable {
    public final int capacity;

    public final double pressureRating;

    public final boolean emergency;

    public CylinderItem(
            Block block,
            Properties properties,
            int capacity,
            double pressureRating,
            boolean emergency) {
        super(block, properties.stacksTo(1));
        this.capacity = capacity;
        this.pressureRating = pressureRating;
        this.emergency = emergency;
    }

    public static CylinderSupply supply(ItemStack stack) {
        CylinderSupply value = stack.getOrDefault(DeepDiving.SUPPLY.get(), CylinderSupply.EMPTY);

        int cap = stack.getItem() instanceof CylinderItem c ? c.capacity : 0;

        return new CylinderSupply(
                value.mix(), Math.min(cap, value.remaining()), value.backupEnabled());
    }

    public double rating() {
        if (emergency) {
            return DivingConfig.EMERGENCY_RATING.get();
        }

        return capacity > 12000
                ? DivingConfig.HIGH_CAPACITY_RATING.get()
                : DivingConfig.ADVANCED_RATING.get();
    }

    @Override
    public String getDescriptionId() {
        return "item.deepdiving."
                + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath();
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            return swapWithEquipmentSlot(this, level, player, hand);
        }

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                CylinderSupply s = supply(stack);

                stack.set(
                        DeepDiving.SUPPLY.get(),
                        new CylinderSupply(s.mix(), s.remaining(), !s.backupEnabled()));
                player.displayClientMessage(
                        Component.translatable(
                                s.backupEnabled()
                                        ? "message.deepdiving.backup_off"
                                        : "message.deepdiving.backup_on"),
                        true);
            }
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        CylinderSupply s = supply(stack);

        lines.add(Component.literal(s.mix().name()).withStyle(ChatFormatting.AQUA));
        lines.add(
                Component.literal(
                                (int) s.remaining() + " / " + capacity + " mB (surface equivalent)")
                        .withStyle(ChatFormatting.GRAY));
        lines.add(
                Component.literal(
                                String.format(
                                        java.util.Locale.ROOT,
                                        "Rating: %.1f atm | Gas MOD: %.0f blocks",
                                        rating(),
                                        Math.floor(
                                                s.mix()
                                                        .maximumDepth(
                                                                DivingConfig.pressureProfile(
                                                                        context.level() == null
                                                                                ? Level.OVERWORLD
                                                                                        .location()
                                                                                : context.level()
                                                                                        .dimension()
                                                                                        .location())))))
                        .withStyle(ChatFormatting.GOLD));

        var profile =
                DivingConfig.pressureProfile(
                        context.level() == null
                                ? Level.OVERWORLD.location()
                                : context.level().dimension().location());

        double minimum = Math.ceil(s.mix().minimumDepth(profile));

        if (minimum > 0) {
            lines.add(
                    Component.literal(
                                    String.format(
                                            java.util.Locale.ROOT,
                                            "Minimum depth: %.0f blocks",
                                            minimum))
                            .withStyle(ChatFormatting.RED));
            lines.add(
                    Component.literal("Low oxygen above this depth").withStyle(ChatFormatting.RED));
        }

        lines.add(
                Component.translatable("tooltip.deepdiving.cylinder_use")
                        .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(
                Component.translatable("tooltip.deepdiving.cylinder_place")
                        .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(
                Component.translatable(
                                s.backupEnabled()
                                        ? "message.deepdiving.backup_on"
                                        : "message.deepdiving.backup_off")
                        .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return supply(stack).remaining() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13 * supply(stack).remaining() / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x50C9BB;
    }
}
