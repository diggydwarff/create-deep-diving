package com.diggydwarff.deepdiving.menu;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class ComputerMenu extends AbstractContainerMenu {
    public ComputerMenu(int id, Inventory inventory) {
        super(DeepDiving.COMPUTER_MENU.get(), id);
    }

    @Override
    public boolean stillValid(Player p) {
        return BreathingEquipment.carries(p, DeepDiving.COMPUTER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        return ItemStack.EMPTY;
    }
}
