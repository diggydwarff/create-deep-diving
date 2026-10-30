package com.diggydwarff.deepdiving.machine;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.compat.GasCompatibility;
import com.diggydwarff.deepdiving.equipment.CylinderItem;
import com.diggydwarff.deepdiving.gas.CylinderSupply;
import com.diggydwarff.deepdiving.gas.GasMix;
import com.diggydwarff.deepdiving.gas.Gases;
import com.diggydwarff.deepdiving.menu.MachineMenu;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/** Shared gas buffers, inventory and menu synchronization for the four machine kinds. */
public final class GasMachine extends KineticBlockEntity implements MenuProvider {
    public static final int CAPACITY = 16000;

    public static final int STORAGE_CAPACITY = 64000;

    public static final int DATA_COUNT = 12;

    public final ItemStackHandler inventory =
            new ItemStackHandler(3) {
                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return slot == 0
                                    && (kind() == MachineBlock.Kind.COMPRESSOR
                                            || kind() == MachineBlock.Kind.BLENDER)
                                    && stack.getItem() instanceof CylinderItem
                            || slot == 2
                                    && (kind() == MachineBlock.Kind.STORAGE
                                            || kind() == MachineBlock.Kind.BLENDER)
                                    && stack.is(DeepDiving.HELIUM_CARTRIDGE.get());
                }

                @Override
                public int getSlotLimit(int slot) {
                    return slot == 2 ? 64 : 1;
                }

                @Override
                protected void onContentsChanged(int slot) {
                    changed();
                }
            };

    public final FluidTank[] tanks = new FluidTank[4];

    public GasMix target = GasMix.AIR;

    public int status;

    private int blendPhase;
    private GasMix blendMix = GasMix.AIR;

    private boolean dirty;

    // These indices are the menu's wire contract; keep them aligned with MachineMenu/Screen.
    public final ContainerData data =
            new ContainerData() {
                @Override
                public int getCount() {
                    return DATA_COUNT;
                }

                @Override
                public void set(int index, int value) {}

                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> (int) Math.abs(getSpeed());
                        case 1 -> status;
                        case 2, 3, 4, 5 -> tanks[index - 2].getFluidAmount() & 0xffff;
                        case 6 -> target.oxygen();
                        case 7 -> target.helium();
                        case 8 ->
                                (int) CylinderItem.supply(inventory.getStackInSlot(0)).remaining();
                        case 9 -> CylinderItem.supply(inventory.getStackInSlot(0)).mix().oxygen();
                        case 10 -> CylinderItem.supply(inventory.getStackInSlot(0)).mix().helium();
                        case 11 ->
                                inventory.getStackInSlot(0).getItem() instanceof CylinderItem c
                                        ? c.capacity
                                        : 0;
                        default -> 0;
                    };
                }
            };

    public GasMachine(BlockPos pos, BlockState state) {
        super(DeepDiving.MACHINE.get(), pos, state);

        for (int i = 0; i < 4; i++) {
            final int index = i;

            tanks[i] =
                    new FluidTank(
                            kind() == MachineBlock.Kind.STORAGE ? STORAGE_CAPACITY : CAPACITY) {
                        @Override
                        public boolean isFluidValid(FluidStack stack) {
                            int gas = GasCompatibility.kind(stack);
                            if (kind() == MachineBlock.Kind.STORAGE && index == 0) {
                                return gas >= 0;
                            }
                            return gas == index;
                        }

                        @Override
                        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
                            return isFluidValid(resource)
                                    ? super.fill(GasCompatibility.canonical(resource), action)
                                    : 0;
                        }

                        @Override
                        public FluidStack drain(
                                FluidStack resource, IFluidHandler.FluidAction action) {
                            FluidStack drained =
                                    super.drain(GasCompatibility.canonical(resource), action);
                            return drained.isEmpty()
                                    ? FluidStack.EMPTY
                                    : resource.copyWithAmount(drained.getAmount());
                        }

                        @Override
                        protected void onContentsChanged() {
                            changed();
                        }
                    };
        }
    }

    public MachineBlock.Kind kind() {
        return ((MachineBlock) getBlockState().getBlock()).kind;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public float calculateStressApplied() {
        return lastStressApplied = kind() == MachineBlock.Kind.STORAGE ? 0 : 4;
    }

    private void changed() {
        dirty = true;
        setChanged();
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide || level.getGameTime() % 10 != 0) {
            return;
        }

        int before = status;

        status = 0;

        // Adjacent containers do not need a pipe network. Pipes still use Create pumps. Only
        // production machines initiate transfers, preventing vessel-to-vessel loops.
        if (kind() != MachineBlock.Kind.STORAGE) {
            transferAdjacent(false);
        }

        if (kind() == MachineBlock.Kind.STORAGE) {
            feedHelium(0);
            status = 7;
        } else if (Math.abs(getSpeed()) < 16) {
            status = 1;
        } else if (level.hasNeighborSignal(worldPosition)) {
            status = 8;
        } else {
            switch (kind()) {
                case COMPRESSOR -> compress();
                case SEPARATOR -> separate();
                case BLENDER -> {
                    feedHelium(3);
                    fillCylinder(target, false);
                }
                default -> {}
            }
        }

        if (kind() != MachineBlock.Kind.STORAGE) {
            transferAdjacent(true);
        }

        if (before != status) {
            dirty = true;
        }

        // Coalesce inventory, fluid and status changes into one block-entity update.
        if (dirty) {
            sendData();
            dirty = false;
        }
    }

    private int rate() {
        return Math.max(1, (int) (Math.abs(getSpeed()) * 2));
    }

    private void compress() {
        if (level.getFluidState(worldPosition.above()).is(FluidTags.WATER)) {
            status = 6;
            return;
        }

        int made =
                tanks[0].fill(
                        new FluidStack(Gases.AIR.source.get(), rate()),
                        IFluidHandler.FluidAction.EXECUTE);

        if (made > 0) {
            status = 2;
        }

        fillCylinder(GasMix.AIR, true);
    }

    private void separate() {
        // Exact 21/79 split. Only commit after both product tanks have room.
        int batches = Math.min(Math.max(1, rate() / 100), tanks[0].getFluidAmount() / 100);

        batches = Math.min(batches, (tanks[1].getCapacity() - tanks[1].getFluidAmount()) / 21);
        batches = Math.min(batches, (tanks[2].getCapacity() - tanks[2].getFluidAmount()) / 79);

        if (batches == 0) {
            status =
                    tanks[0].getFluidAmount() < 100
                            ? 9
                            : tanks[1].getCapacity() - tanks[1].getFluidAmount() < 21 ? 10 : 11;
            return;
        }

        tanks[0].drain(batches * 100, IFluidHandler.FluidAction.EXECUTE);
        tanks[1].fill(
                new FluidStack(Gases.OXYGEN.source.get(), batches * 21),
                IFluidHandler.FluidAction.EXECUTE);
        tanks[2].fill(
                new FluidStack(Gases.NITROGEN.source.get(), batches * 79),
                IFluidHandler.FluidAction.EXECUTE);
        status = 2;
    }

    /** Transfer through the same sided capabilities that pumps use, without loading chunks. */
    private void transferAdjacent(boolean outputs) {
        for (Direction side : Direction.values()) {
            IFluidHandler own = fluidPort(side);

            if (!(own instanceof Port port) || (outputs ? !port.output : !port.input)) {
                continue;
            }

            BlockPos neighbor = worldPosition.relative(side);

            if (!level.hasChunkAt(neighbor)) {
                continue;
            }

            IFluidHandler other =
                    level.getCapability(
                            Capabilities.FluidHandler.BLOCK, neighbor, side.getOpposite());

            if (other == null) {
                continue;
            }

            IFluidHandler source = outputs ? own : other;

            IFluidHandler destination = outputs ? other : own;

            for (int tank = 0; tank < source.getTanks(); tank++) {
                FluidStack offered = source.getFluidInTank(tank);

                if (offered.isEmpty()) {
                    continue;
                }

                offered = offered.copyWithAmount(Math.min(rate(), offered.getAmount()));

                FluidStack simulated = source.drain(offered, IFluidHandler.FluidAction.SIMULATE);

                int accepted = destination.fill(simulated, IFluidHandler.FluidAction.SIMULATE);

                if (accepted == 0) {
                    continue;
                }

                FluidStack moved =
                        source.drain(
                                simulated.copyWithAmount(accepted),
                                IFluidHandler.FluidAction.EXECUTE);

                int filled = destination.fill(moved, IFluidHandler.FluidAction.EXECUTE);

                // Standard handlers honor SIMULATE. Retain anything an external handler
                // unexpectedly refuses.
                if (filled < moved.getAmount()) {
                    FluidStack remainder = moved.copyWithAmount(moved.getAmount() - filled);

                    if (outputs) {
                        port.tank.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
                    } else {
                        source.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
                    }
                }

                break;
            }
        }
    }

    private void feedHelium(int tank) {
        ItemStack cartridge = inventory.getStackInSlot(2);

        if (!cartridge.is(DeepDiving.HELIUM_CARTRIDGE.get())) {
            return;
        }

        FluidStack gas = new FluidStack(Gases.HELIUM.source.get(), 1000);

        if (tanks[tank].fill(gas, IFluidHandler.FluidAction.SIMULATE) != 1000) {
            return;
        }

        cartridge.shrink(1);
        tanks[tank].fill(gas, IFluidHandler.FluidAction.EXECUTE);
        changed();
    }

    private void fillCylinder(GasMix mix, boolean compressedAir) {
        ItemStack stack = inventory.getStackInSlot(0);

        if (!(stack.getItem() instanceof CylinderItem cylinder)) {
            return;
        }

        if (!inventory.getStackInSlot(1).isEmpty()) {
            status = 5;
            return;
        }

        CylinderSupply supply = CylinderItem.supply(stack);

        if (supply.remaining() > 0 && !supply.mix().equals(mix)) {
            status = 4;
            return;
        }

        int amount = Math.min(rate(), (int) Math.floor(cylinder.capacity - supply.remaining()));

        if (amount <= 0) {
            finishCylinder();
            return;
        }

        if (compressedAir) {
            amount = Math.min(amount, tanks[0].getFluidAmount());

            if (amount == 0) {
                status = 3;
                return;
            }

            tanks[0].drain(amount, IFluidHandler.FluidAction.EXECUTE);
        } else {
            if (!mix.equals(blendMix)) {
                blendMix = mix;
                blendPhase = 0;
            }
            amount =
                    mix.availableAmount(
                            amount,
                            new int[] {
                                tanks[1].getFluidAmount(),
                                tanks[2].getFluidAmount(),
                                tanks[3].getFluidAmount()
                            },
                            blendPhase);
            if (amount == 0) {
                status = 3;
                return;
            }
            int[] needed = mix.constituents(amount, blendPhase);

            for (int i = 0; i < 3; i++) {
                tanks[i + 1].drain(needed[i], IFluidHandler.FluidAction.EXECUTE);
            }
            blendPhase = (blendPhase + amount) % 100;
        }

        stack.set(
                DeepDiving.SUPPLY.get(),
                new CylinderSupply(
                        mix,
                        Math.min(cylinder.capacity, supply.remaining() + amount),
                        supply.backupEnabled()));
        status = 2;
        changed();

        if (cylinder.capacity - CylinderItem.supply(stack).remaining() < 1) {
            finishCylinder();
        }
    }

    private void finishCylinder() {
        inventory.setStackInSlot(1, inventory.getStackInSlot(0));
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        changed();
    }

    public void button(int id) {
        switch (id) {
            case 0 -> target = GasMix.AIR;
            case 1 -> target = GasMix.NITROX;
            case 2 -> target = GasMix.TRIMIX;
            case 3 -> target = GasMix.HELIOX;
            case 4 -> {
                if (!inventory.getStackInSlot(0).isEmpty()) {
                    var s = CylinderItem.supply(inventory.getStackInSlot(0));

                    inventory
                            .getStackInSlot(0)
                            .set(
                                    DeepDiving.SUPPLY.get(),
                                    new CylinderSupply(s.mix(), 0, s.backupEnabled()));
                }
            }
            case 5 -> target = new GasMix(Math.max(1, target.oxygen() - 1), target.helium());
            case 6 ->
                    target =
                            new GasMix(
                                    Math.min(100 - target.helium(), target.oxygen() + 1),
                                    target.helium());
            case 7 -> target = new GasMix(target.oxygen(), Math.max(0, target.helium() - 1));
            case 8 ->
                    target =
                            new GasMix(
                                    target.oxygen(),
                                    Math.min(100 - target.oxygen(), target.helium() + 1));
            default -> {
                return;
            }
        }
        changed();
        sendData();
    }

    public IFluidHandler fluidPort(Direction side) {
        if (kind() == MachineBlock.Kind.STORAGE) {
            return new Port(tanks[0], true, true);
        }

        var front = getBlockState().getValue(MachineBlock.HORIZONTAL_FACING);

        if (kind() == MachineBlock.Kind.COMPRESSOR) {
            return side == front.getOpposite() ? null : new Port(tanks[0], false, true);
        }

        if (kind() == MachineBlock.Kind.SEPARATOR) {
            if (side == Direction.UP) {
                return new Port(tanks[0], true, false);
            }

            if (side == front.getCounterClockWise()) {
                return new Port(tanks[1], false, true);
            }

            if (side == front.getClockWise()) {
                return new Port(tanks[2], false, true);
            }
        }

        if (kind() == MachineBlock.Kind.BLENDER) {
            if (side == front.getCounterClockWise()) {
                return new Port(tanks[1], true, false);
            }

            if (side == front.getClockWise()) {
                return new Port(tanks[2], true, false);
            }

            if (side == Direction.UP) {
                return new Port(tanks[3], true, false);
            }
        }

        return null;
    }

    public IItemHandler itemPort(Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 3;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inventory.getStackInSlot(slot);
            }

            @Override
            public int getSlotLimit(int slot) {
                return inventory.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return inventory.isItemValid(slot, stack);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot == 1 || side == Direction.DOWN
                        ? stack
                        : inventory.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 1 && (side == null || side == Direction.DOWN)
                        ? inventory.extractItem(slot, amount, simulate)
                        : ItemStack.EMPTY;
            }
        };
    }

    private record Port(FluidTank tank, boolean input, boolean output) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int i) {
            return tank.getFluidInTank(i);
        }

        @Override
        public int getTankCapacity(int i) {
            return tank.getTankCapacity(i);
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluid) {
            return input && tank.isFluidValid(i, fluid);
        }

        @Override
        public int fill(FluidStack fluid, FluidAction action) {
            return input ? tank.fill(fluid, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack fluid, FluidAction action) {
            return output ? tank.drain(fluid, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return output ? tank.drain(amount, action) : FluidStack.EMPTY;
        }
    }

    public CompoundTag saveCargo(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        tag.put("inventory", inventory.serializeNBT(provider));

        for (int i = 0; i < 4; i++) {
            tag.put("tank" + i, tanks[i].writeToNBT(provider, new CompoundTag()));
        }

        tag.putInt("oxygen", target.oxygen());
        tag.putInt("helium", target.helium());
        tag.putInt("blendPhase", target.equals(blendMix) ? blendPhase : 0);
        return tag;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider provider, boolean packet) {
        super.write(tag, provider, packet);
        tag.merge(saveCargo(provider));
        tag.putInt("status", status);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider provider, boolean packet) {
        super.read(tag, provider, packet);

        if (tag.contains("inventory")) {
            inventory.deserializeNBT(provider, tag.getCompound("inventory"));
        }

        for (int i = 0; i < 4; i++) {
            if (tag.contains("tank" + i)) {
                tanks[i].readFromNBT(provider, tag.getCompound("tank" + i));
            }
        }

        int o = Math.clamp(tag.contains("oxygen") ? tag.getInt("oxygen") : 21, 1, 100);

        int h = Math.clamp(tag.getInt("helium"), 0, 100 - o);

        target = new GasMix(o, h);
        blendMix = target;
        blendPhase = Math.clamp(tag.getInt("blendPhase"), 0, 99);
        status = tag.getInt("status");
    }

    public void writeMenuData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        buf.writeVarInt(kind().ordinal());
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, tanks[0].getFluid());
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineMenu(id, inv, this);
    }

    public static String statusText(int status) {
        return switch (status) {
            case 1 -> "Needs 16+ RPM";
            case 2 -> "Running";
            case 3 -> "Needs gas / output space";
            case 4 -> "Purge cylinder to change mixture";
            case 5 -> "Output occupied";
            case 6 -> "Intake is underwater";
            case 7 -> "Gas storage ready";
            case 8 -> "Redstone paused";
            case 9 -> "Needs compressed air at top inlet";
            case 10 -> "Oxygen output buffer full";
            case 11 -> "Nitrogen output buffer full";
            default -> "Idle";
        };
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(
                Component.literal("      " + getDisplayName().getString())
                        .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  " + statusText(status)).withStyle(ChatFormatting.GRAY));

        if (kind() != MachineBlock.Kind.STORAGE) {
            Direction front = getBlockState().getValue(MachineBlock.HORIZONTAL_FACING);

            tooltip.add(
                    Component.literal("  Shaft: " + front.getOpposite().getName())
                            .withStyle(ChatFormatting.DARK_GRAY));

            if (kind() == MachineBlock.Kind.SEPARATOR || kind() == MachineBlock.Kind.BLENDER) {
                tooltip.add(
                        Component.literal(
                                        "  O2 "
                                                + (kind() == MachineBlock.Kind.SEPARATOR
                                                        ? "OUT: "
                                                        : "IN: ")
                                                + front.getCounterClockWise().getName()
                                                + " | N2 "
                                                + (kind() == MachineBlock.Kind.SEPARATOR
                                                        ? "OUT: "
                                                        : "IN: ")
                                                + front.getClockWise().getName())
                                .withStyle(ChatFormatting.AQUA));
                tooltip.add(
                        Component.literal(
                                        kind() == MachineBlock.Kind.SEPARATOR
                                                ? "  Compressed air IN: top"
                                                : "  Helium IN: top")
                                .withStyle(ChatFormatting.GRAY));
            }
        }

        for (FluidTank tank : tanks) {
            if (!tank.isEmpty()) {
                tooltip.add(
                        Component.literal(
                                        "  "
                                                + tank.getFluid().getHoverName().getString()
                                                + ": "
                                                + tank.getFluidAmount()
                                                + " mB")
                                .withStyle(ChatFormatting.AQUA));
            }
        }

        if (kind() == MachineBlock.Kind.BLENDER) {
            tooltip.add(Component.literal("  " + target.summary()).withStyle(ChatFormatting.GRAY));
        }

        if (kind() == MachineBlock.Kind.STORAGE) {
            tooltip.add(
                    Component.literal(
                                    String.format(
                                            java.util.Locale.ROOT,
                                            "  %.1f / 200 atm | 64000 mB capacity",
                                            1
                                                    + 199.0
                                                            * tanks[0].getFluidAmount()
                                                            / STORAGE_CAPACITY))
                            .withStyle(ChatFormatting.GRAY));
        }

        return true;
    }
}
