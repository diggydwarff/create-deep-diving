package com.diggydwarff.deepdiving.menu;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.equipment.CylinderItem;
import com.diggydwarff.deepdiving.machine.GasMachine;
import com.diggydwarff.deepdiving.machine.MachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class MachineMenu extends AbstractContainerMenu {
    public final GasMachine machine;

    public final BlockPos pos;

    public final ContainerData data;

    public final MachineBlock.Kind kind;

    private final Inventory playerInventory;

    public MachineMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, clientMachine(inv, buf), clientData());
    }

    private static ContainerData clientData() {
        var data = new SimpleContainerData(GasMachine.DATA_COUNT);

        data.set(6, 21);
        return data;
    }

    private static GasMachine clientMachine(Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();

        int kind = buf.readVarInt();

        var block = switch (kind) {
            case 0 -> DeepDiving.COMPRESSOR.get();
            case 1 -> DeepDiving.SEPARATOR.get();
            case 2 -> DeepDiving.BLENDER.get();
            case 3 -> DeepDiving.STORAGE.get();
            default -> throw new IllegalArgumentException("Invalid gas machine kind");
        };

        // Menus can arrive before chunk/block-entity packets. A local inventory avoids that race.
        var machine = new GasMachine(pos, block.defaultBlockState());

        machine.setLevel(inventory.player.level());
        machine.tanks[0].setFluid(net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC.decode(buf));
        return machine;
    }

    public com.diggydwarff.deepdiving.gas.GasMix targetMixture() {
        // Container fields arrive separately; keep transitional client display values valid.
        int oxygen = Math.clamp(data.get(6), 1, 100);

        return new com.diggydwarff.deepdiving.gas.GasMix(oxygen, Math.clamp(data.get(7), 0, 100 - oxygen));
    }

    public net.neoforged.neoforge.fluids.FluidStack storedFluid() {
        var loaded = machine.getLevel().getBlockEntity(pos);

        return loaded instanceof GasMachine live
                ? live.tanks[0].getFluid()
                : machine.tanks[0].getFluid();
    }

    public MachineMenu(int id, Inventory inv, GasMachine machine) {
        this(id, inv, machine, machine.data);
    }

    private MachineMenu(
        int id,
        Inventory inv,
        GasMachine machine,
        ContainerData data
    ) {
        super(DeepDiving.MACHINE_MENU.get(), id);

        if (machine == null) {
            throw new IllegalStateException("Gas machine is no longer loaded");
        }

        this.machine = machine;
        this.pos = machine.getBlockPos();
        this.kind = machine.kind();
        this.data = data;
        this.playerInventory = inv;
        addSlot(new SlotItemHandler(machine.inventory, 0, filling(machine) ? 24 : -1000, 100));
        addSlot(
            new SlotItemHandler(machine.inventory, 1, filling(machine) ? 61 : -1000, 100) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            }
        );
        addSlot(
            new SlotItemHandler(
                machine.inventory,
                2,
                (kind == MachineBlock.Kind.BLENDER || kind == MachineBlock.Kind.STORAGE)
                        ? 190
                        : -1000,
                100
            )
        );

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 35 + col * 18, 150 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 35 + col * 18, 208));
        }

        addDataSlots(data);
        arrangeSlots(MachineLayout.SPACIOUS);
    }

    /** Only screen coordinates change; slot identities and network/inventory indices stay fixed. */
    public void arrangeSlots(MachineLayout layout) {
        replaceSlot(
            0,
            new SlotItemHandler(
                machine.inventory,
                0,
                filling(machine) ? 24 : -1000,
                layout.slotsY
            )
        );
        replaceSlot(
            1,
            new SlotItemHandler(
                machine.inventory,
                1,
                filling(machine) ? 68 : -1000,
                layout.slotsY
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            }
        );
        replaceSlot(
            2,
            new SlotItemHandler(
                machine.inventory,
                2,
                kind == MachineBlock.Kind.BLENDER || kind == MachineBlock.Kind.STORAGE
                        ? 232
                        : -1000,
                layout.slotsY
            )
        );

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                replaceSlot(
                    3 + row * 9 + col,
                    new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        55 + col * 18,
                        layout.inventoryY + 18 + row * 18
                    )
                );
            }
        }

        for (int col = 0; col < 9; col++) {
            replaceSlot(30 + col, new Slot(playerInventory, col, 55 + col * 18, layout.inventoryY + 76));
        }
    }

    private void replaceSlot(int index, Slot slot) {
        slot.index = index;
        slots.set(index, slot);
    }

    private static boolean filling(GasMachine m) {
        return m.kind() == MachineBlock.Kind.COMPRESSOR || m.kind() == MachineBlock.Kind.BLENDER;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.level().isClientSide
            || p.level().getBlockEntity(pos) == machine
                && p.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) < 64;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!stillValid(p) || id < 0 || id > 8 || kind != MachineBlock.Kind.BLENDER && id != 4) {
            return false;
        }

        machine.button(id);
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();

        ItemStack copy = source.copy();

        if (index < 3) {
            if (!moveItemStackTo(source, 3, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (source.getItem() instanceof CylinderItem) {
            if (!moveItemStackTo(source, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (source.is(DeepDiving.HELIUM_CARTRIDGE.get())) {
            if (!moveItemStackTo(source, 2, 3, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 30) {
            if (!moveItemStackTo(source, 30, 39, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 3, 30, false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(p, source);
        return copy;
    }
}
