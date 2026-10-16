package com.diggydwarff.deepdiving.guideline;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public final class GuideLineItem extends Item {
    public final DyeColor color;

    public GuideLineItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel()
            .getBlockState(context.getClickedPos())
            .is(DeepDiving.GUIDE_NODE.get())) {
            return InteractionResult.PASS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        if (!player.mayBuild()) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        BlockPos clicked = context.getClickedPos();

        String dimension = player.level().dimension().location().toString();

        if (!tag.contains("anchor") || !dimension.equals(tag.getString("dimension"))) {
            tag.putLong("anchor", clicked.asLong());
            tag.putString("dimension", dimension);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.displayClientMessage(
                Component.literal("First anchor selected. Use this line on the next anchor (up to 64 blocks)."),
                true
            );
            return InteractionResult.SUCCESS;
        }

        BlockPos first = BlockPos.of(tag.getLong("anchor"));

        GuideLine line = GuideNetwork.get(player.serverLevel())
            .connect(
                player.serverLevel(),
                first,
                clicked,
                color,
                tag.getBoolean("glowing")
            );

        if (line == null) {
            player.displayClientMessage(
                Component.literal(
                    "Cannot connect: use two loaded anchors within 64 blocks, with room for another line."
                ),
                true
            );
            return InteractionResult.FAIL;
        }

        tag.remove("anchor");
        tag.remove("dimension");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        player.displayClientMessage(Component.literal("Guide line connected."), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            tag.remove("anchor");
            tag.remove("dimension");
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.displayClientMessage(Component.literal("Guide line selection cleared."), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static ItemStack refund(GuideLine line) {
        ItemStack stack = new ItemStack(DeepDiving.GUIDE_LINES.get(line.color()).get());

        if (line.glowing()) {
            CompoundTag tag = new CompoundTag();

            tag.putBoolean("glowing", true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        return stack;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        List<Component> lines,
        TooltipFlag flag
    ) {
        lines.add(Component.literal("Use on two guide nodes to connect. Maximum span: 64 blocks."));
        lines.add(Component.literal("Glow ink: coat one line | Knife/shears: cut one line"));
        lines.add(Component.literal("Sneak-use in air: clear selected anchor"));

        if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag()
            .getBoolean("glowing")) {
            lines.add(Component.literal("Glow-coated line"));
        }
    }
}
