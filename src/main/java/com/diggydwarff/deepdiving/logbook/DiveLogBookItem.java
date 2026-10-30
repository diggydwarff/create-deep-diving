package com.diggydwarff.deepdiving.logbook;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DiveLogBookItem extends Item {
    public DiveLogBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown())
                DiveLogService.toggle(serverPlayer, player.getItemInHand(hand));
            DiveLogService.open(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        var log = DiveLogService.read(stack);
        lines.add(
                Component.translatable(
                        log.getBoolean("recording")
                                ? "tooltip.deepdiving.dive_log.recording"
                                : "tooltip.deepdiving.dive_log.use"));
        lines.add(Component.translatable("tooltip.deepdiving.dive_log.controls"));
        lines.add(
                Component.translatable(
                        "tooltip.deepdiving.dive_log.entries",
                        log.getList("dives", Tag.TAG_COMPOUND).size(),
                        DiveLogService.MAX_ENTRIES));
    }
}
