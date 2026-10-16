package com.diggydwarff.deepdiving.equipment;

import com.diggydwarff.deepdiving.menu.ComputerMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DivingComputerItem extends Item {
    public DivingComputerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand hand
    ) {
        if (!level.isClientSide) {
            player.openMenu(
                new SimpleMenuProvider(
                    (id, inventory, owner) -> new ComputerMenu(id, inventory),
                    Component.translatable("item.deepdiving.diving_computer")
                )
            );
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
