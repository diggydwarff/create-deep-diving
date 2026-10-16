package com.diggydwarff.deepdiving.qa;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.equipment.*;
import com.diggydwarff.deepdiving.gas.*;
import com.diggydwarff.deepdiving.guideline.*;
import com.diggydwarff.deepdiving.machine.GasMachine;
import com.diggydwarff.deepdiving.machine.MachineBlock;
import com.diggydwarff.deepdiving.network.*;
import com.simibubi.create.AllItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import io.netty.buffer.Unpooled;

/** Opt-in checks in an actual dedicated server. Compiled only into the QA source set. */
@EventBusSubscriber(modid = DeepDiving.MOD_ID)
public final class ServerProbe {
    private static boolean pending;

    private static final List<String> results = new ArrayList<>();

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        pending = Boolean.getBoolean("deepdiving.qa");
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!pending) return;
        pending = false;

        ServerLevel level = event.getServer().overworld();
        BlockPos origin = level.getSharedSpawnPos().above(2);

        run("deterministic simulation suite inside loaded server", () -> SimulationTest.main(new String[0]));
        run("registered content and machine data contract", () -> {
            check(DeepDiving.ADVANCED.get() != null && DeepDiving.LAMP.get() != null);
            check(DeepDiving.GUIDE_LINES.size() == 16);
            check(Gases.all().length == 4 && GasMachine.DATA_COUNT == 12);
        });
        run("all seven packet codecs round-trip and reject invalid sediment", () -> packets(level));
        run("breathing selects and drains exactly one supply", () -> equipment(level));
        run("death clears exposure and End return preserves it", () -> respawn(level));
        run("active lamp preserves night vision owned by another system", () -> nightVision(level));
        run("separator conserves input gas and refuses partial transactions", () -> separator(level, origin));
        run("blender conserves ingredients and cannot partly fill without gas", () -> blender(level, origin));
        run("machine capabilities preserve sided input/output and simulation", () -> ports(level, origin));
        run("machine cargo and cylinder components survive serialization", () -> cargo(level, origin));
        run("guide network removes one authoritative edge only once", () -> guideline(level, origin));
        run("legacy lamp cleanup restores water/air and leaves ordinary blocks", () -> legacy(level, origin));
        run("legacy sweep queues only affected chunks before live lookup is ready", () -> migrationQueue(level, event));

        try {
            Files.write(Path.of("qa-results.txt"), results);
        } catch (Exception failure) {
            throw new RuntimeException("Could not save dedicated-server QA report", failure);
        } finally {
            event.getServer().halt(false);
        }
    }

    private static void run(String name, Checked test) {
        try {
            test.run();
            results.add("PASS " + name);
        } catch (Throwable failure) {
            results.add("FAIL " + name + ": " + failure);
            failure.printStackTrace();
        }
        System.out.println(results.getLast());
    }

    private static void check(boolean value) {
        if (!value) throw new AssertionError("Unexpected server state");
    }

    private static FakePlayer player(ServerLevel level) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), "DivingQa"));
    }

    private static ItemStack cylinder(int amount) {
        ItemStack stack = new ItemStack(DeepDiving.ADVANCED.get());
        stack.set(DeepDiving.SUPPLY.get(), new CylinderSupply(GasMix.TRIMIX, amount, true));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("QA cylinder"));
        return stack;
    }

    private static <T> void roundTrip(ServerLevel level, StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());

        try {
            codec.encode(buffer, value);
            check(value.equals(codec.decode(buffer)) && !buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    private static void packets(ServerLevel level) {
        var dimension = level.dimension().location();
        var uuid = UUID.randomUUID();

        // Distinct values catch swapped fields as well as omitted trailing data.
        roundTrip(level, DivingPackets.Snapshot.CODEC, new DivingPackets.Snapshot(
            41.25, 5.1, 3001.5, 12000, -0.25, 99, 0.3, 0.4, 0.5, 6,
            0.6, 0.7, 123, 18, 45, 0x7ff, true, 500, 0.15, 72,
            true, 40.5, -12, true, false, 31.5, -3));
        roundTrip(level, LampState.CODEC, new LampState(uuid, dimension, true, false));
        roundTrip(level, BreathingPulse.CODEC, new BreathingPulse(451, uuid, 2, 1, 37, true));
        roundTrip(level, SedimentPulse.CODEC, new SedimentPulse(dimension, new BlockPos(-23, -40, 17), true, 1.25f));
        roundTrip(level, DivingPackets.DepthHoldRequest.CODEC, new DivingPackets.DepthHoldRequest(true));
        roundTrip(level, DivingPackets.FinControl.CODEC, new DivingPackets.FinControl(true, false, true));
        roundTrip(level, DivingPackets.TreadingPose.CODEC, new DivingPackets.TreadingPose(723, true, false, true));

        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, -0.01f, 4.01f}) {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());

            try {
                SedimentPulse.CODEC.encode(buffer, new SedimentPulse(dimension, BlockPos.ZERO, false, invalid));
                boolean rejected = false;

                try {
                    SedimentPulse.CODEC.decode(buffer);
                } catch (IllegalArgumentException expected) {
                    rejected = true;
                }

                check(rejected);
            } finally {
                buffer.release();
            }
        }
    }

    private static void migrationQueue(ServerLevel level, ServerTickEvent.Post event) throws Exception {
        var field = DivingLampService.class.getDeclaredField("CLEANUP");
        field.setAccessible(true);
        var queue = (java.util.Queue<?>) field.get(null);
        int before = queue.size();
        var position = new net.minecraft.world.level.ChunkPos(28000, 28000);
        var chunk = new net.minecraft.world.level.chunk.LevelChunk(level, position);

        check(level.getChunkSource().getChunkNow(position.x, position.z) == null);
        DivingLampService.loaded(new net.neoforged.neoforge.event.level.ChunkEvent.Load(chunk, false));
        check(queue.size() == before);
        chunk.getSections()[0].setBlockState(1, 1, 1, DeepDiving.LAMP_LIGHT.get().defaultBlockState());
        DivingLampService.loaded(new net.neoforged.neoforge.event.level.ChunkEvent.Load(chunk, false));
        check(queue.size() == before + 1);
        DivingLampService.migrate(event);
        check(queue.size() <= before && level.getChunkSource().getChunkNow(position.x, position.z) == null);
    }

    private static void equipment(ServerLevel level) {
        FakePlayer diver = player(level);
        diver.setItemSlot(EquipmentSlot.HEAD, AllItems.COPPER_DIVING_HELMET.asStack());
        diver.getInventory().items.set(0, new ItemStack(DeepDiving.REGULATOR.get()));
        ItemStack worn = cylinder(100);
        ItemStack backup = cylinder(200);
        diver.setItemSlot(EquipmentSlot.CHEST, worn);
        diver.getInventory().items.set(1, backup);
        var selected = BreathingEquipment.select(diver);
        check(selected != null && selected.stack() == worn);
        BreathingEquipment.consume(diver, selected, 30);
        check(CylinderItem.supply(worn).remaining() == 70 && CylinderItem.supply(backup).remaining() == 200);
        BreathingEquipment.consume(diver, selected, 100);
        check(BreathingEquipment.select(diver).stack() == backup);
        diver.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        check(BreathingEquipment.select(diver) == null);
    }

    private static void respawn(ServerLevel level) {
        FakePlayer old = player(level);
        FakePlayer next = new FakePlayer(level, old.getGameProfile());
        var state = new com.diggydwarff.deepdiving.physiology.DiverState();
        state.oxygenation = 42;
        old.getPersistentData().put("deepdiving.physiology", state.save());
        next.getPersistentData().put("deepdiving.physiology", state.save());
        next.getPersistentData().putBoolean("otherMod", true);
        DiverService.clone(new PlayerEvent.Clone(next, old, true));
        check(!next.getPersistentData().contains("deepdiving.physiology") && next.getPersistentData().getBoolean("otherMod"));
        DiverService.clone(new PlayerEvent.Clone(next, old, false));
        check(next.getPersistentData().getCompound("deepdiving.physiology").getDouble("oxygenation") == 42);
    }

    private static void nightVision(ServerLevel level) {
        FakePlayer diver = player(level);
        diver.getInventory().items.set(0, new ItemStack(DeepDiving.LAMP.get()));
        diver.getPersistentData().putBoolean("deepdiving.lamp", true);
        diver.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, false, false, false));
        DivingLampService.update(diver);
        check(diver.hasEffect(MobEffects.NIGHT_VISION));
    }

    private static GasMachine machine(ServerLevel level, BlockPos origin, MachineBlock.Kind kind) {
        MachineBlock block = switch (kind) {
            case COMPRESSOR -> DeepDiving.COMPRESSOR.get();
            case SEPARATOR -> DeepDiving.SEPARATOR.get();
            case BLENDER -> DeepDiving.BLENDER.get();
            case STORAGE -> DeepDiving.STORAGE.get();
        };
        var machine = new GasMachine(origin, block.defaultBlockState());
        machine.setLevel(level);
        machine.setSpeed(64);
        return machine;
    }

    private static void call(GasMachine machine, String name, Class<?>[] types, Object... args) throws Exception {
        var method = GasMachine.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(machine, args);
    }

    private static void separator(ServerLevel level, BlockPos origin) throws Exception {
        GasMachine machine = machine(level, origin, MachineBlock.Kind.SEPARATOR);
        machine.tanks[0].fill(new FluidStack(Gases.AIR.source.get(), 100), FluidAction.EXECUTE);
        call(machine, "separate", new Class<?>[0]);
        check(machine.tanks[0].isEmpty());
        check(machine.tanks[1].getFluidAmount() == 21 && machine.tanks[2].getFluidAmount() == 79);
        machine.tanks[0].fill(new FluidStack(Gases.AIR.source.get(), 100), FluidAction.EXECUTE);
        machine.tanks[1].fill(new FluidStack(Gases.OXYGEN.source.get(), GasMachine.CAPACITY), FluidAction.EXECUTE);
        call(machine, "separate", new Class<?>[0]);
        check(machine.tanks[0].getFluidAmount() == 100 && machine.tanks[2].getFluidAmount() == 79);
    }

    private static void blender(ServerLevel level, BlockPos origin) throws Exception {
        GasMachine machine = machine(level, origin, MachineBlock.Kind.BLENDER);
        ItemStack cylinder = cylinder(0);
        machine.inventory.setStackInSlot(0, cylinder);
        int[] needed = GasMix.TRIMIX.constituents(128);
        machine.tanks[1].fill(new FluidStack(Gases.OXYGEN.source.get(), needed[0]), FluidAction.EXECUTE);
        machine.tanks[2].fill(new FluidStack(Gases.NITROGEN.source.get(), needed[1]), FluidAction.EXECUTE);
        call(machine, "fillCylinder", new Class<?>[]{GasMix.class, boolean.class}, GasMix.TRIMIX, false);
        check(CylinderItem.supply(cylinder).remaining() == 0 && machine.tanks[1].getFluidAmount() == needed[0]);
        machine.tanks[3].fill(new FluidStack(Gases.HELIUM.source.get(), needed[2]), FluidAction.EXECUTE);
        call(machine, "fillCylinder", new Class<?>[]{GasMix.class, boolean.class}, GasMix.TRIMIX, false);
        check(CylinderItem.supply(cylinder).remaining() == 128);
        check(machine.tanks[1].isEmpty() && machine.tanks[2].isEmpty() && machine.tanks[3].isEmpty());
    }

    private static void ports(ServerLevel level, BlockPos origin) {
        GasMachine machine = machine(level, origin, MachineBlock.Kind.SEPARATOR);
        var input = machine.fluidPort(Direction.UP);
        check(input.fill(new FluidStack(Gases.AIR.source.get(), 100), FluidAction.SIMULATE) == 100);
        check(machine.tanks[0].isEmpty());
        input.fill(new FluidStack(Gases.AIR.source.get(), 100), FluidAction.EXECUTE);
        check(input.drain(100, FluidAction.EXECUTE).isEmpty());
        check(input.fill(new FluidStack(Gases.HELIUM.source.get(), 1), FluidAction.EXECUTE) == 0);
        GasMachine storage = machine(level, origin, MachineBlock.Kind.STORAGE);
        storage.tanks[0].fill(new FluidStack(Gases.HELIUM.source.get(), 64000), FluidAction.EXECUTE);
        check(storage.data.get(2) == 64000);
    }

    private static void cargo(ServerLevel level, BlockPos origin) {
        GasMachine original = machine(level, origin, MachineBlock.Kind.BLENDER);
        original.target = GasMix.HELIOX;
        ItemStack item = cylinder(4321);
        original.inventory.setStackInSlot(0, item);
        original.tanks[3].fill(new FluidStack(Gases.HELIUM.source.get(), 1200), FluidAction.EXECUTE);
        GasMachine loaded = machine(level, origin, MachineBlock.Kind.BLENDER);
        loaded.loadWithComponents(original.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
        check(loaded.target.equals(GasMix.HELIOX) && loaded.tanks[3].getFluidAmount() == 1200);
        check(ItemStack.isSameItemSameComponents(loaded.inventory.getStackInSlot(0), item));
        var placed = new CylinderBlockEntity(origin, DeepDiving.ADVANCED_BLOCK.get().defaultBlockState());
        placed.setItem(item);
        var copy = new CylinderBlockEntity(origin, DeepDiving.ADVANCED_BLOCK.get().defaultBlockState());
        copy.loadWithComponents(placed.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
        check(ItemStack.isSameItemSameComponents(copy.item(), item));
    }

    private static void guideline(ServerLevel level, BlockPos origin) {
        BlockPos next = origin.east(3);
        level.setBlockAndUpdate(origin, DeepDiving.GUIDE_NODE.get().defaultBlockState());
        level.setBlockAndUpdate(next, DeepDiving.GUIDE_NODE.get().defaultBlockState());
        var network = GuideNetwork.get(level);
        var line = network.connect(level, origin, next, DyeColor.RED, true);
        check(line != null && network.at(origin).size() == 1 && network.at(next).size() == 1);
        check(network.remove(level, line.id()) != null && network.remove(level, line.id()) == null);
        check(network.at(origin).isEmpty() && network.at(next).isEmpty());
        level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(next, Blocks.AIR.defaultBlockState());
    }

    private static void legacy(ServerLevel level, BlockPos origin) {
        level.setBlockAndUpdate(origin, DeepDiving.LAMP_LIGHT.get().defaultBlockState().setValue(LightBlock.WATERLOGGED, true));
        DivingLampService.restore(level, origin);
        check(level.getBlockState(origin).is(Blocks.WATER));
        level.setBlockAndUpdate(origin, DeepDiving.LAMP_LIGHT.get().defaultBlockState());
        DivingLampService.restore(level, origin);
        check(level.getBlockState(origin).isAir());
        level.setBlockAndUpdate(origin, Blocks.STONE.defaultBlockState());
        DivingLampService.restore(level, origin);
        check(level.getBlockState(origin).is(Blocks.STONE));
        level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }

    private ServerProbe() {}
}
