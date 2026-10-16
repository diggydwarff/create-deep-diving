package com.diggydwarff.deepdiving;

import com.diggydwarff.deepdiving.compat.GasDisplaySource;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.equipment.*;
import com.diggydwarff.deepdiving.gas.*;
import com.diggydwarff.deepdiving.guideline.*;
import com.diggydwarff.deepdiving.machine.*;
import com.diggydwarff.deepdiving.menu.*;
import com.diggydwarff.deepdiving.network.DivingPackets;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DeepDiving.MOD_ID)
public final class DeepDiving {
    public static final String MOD_ID = "deepdiving";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES =
        DeferredRegister.create(CreateBuiltInRegistries.DISPLAY_SOURCE.key(), MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CylinderSupply>>
            SUPPLY = COMPONENTS.register(
                "cylinder_supply",
                () -> DataComponentType.<CylinderSupply>builder()
                    .persistent(CylinderSupply.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(CylinderSupply.CODEC))
                    .build()
            );

    public static final DeferredHolder<Block, CylinderBlock> ADVANCED_BLOCK = cylinderBlock("advanced_diving_cylinder");

    public static final DeferredHolder<Block, CylinderBlock> HIGH_CAPACITY_BLOCK =
        cylinderBlock("high_capacity_diving_cylinder");

    public static final DeferredHolder<Block, CylinderBlock> EMERGENCY_BLOCK = cylinderBlock("emergency_air_cylinder");

    public static final DeferredHolder<Item, CylinderItem> ADVANCED = ITEMS.register(
        "advanced_diving_cylinder",
        () -> new CylinderItem(
            ADVANCED_BLOCK.get(),
            new Item.Properties(),
            12000,
            12,
            false
        )
    );

    public static final DeferredHolder<Item, CylinderItem> HIGH_CAPACITY = ITEMS.register(
        "high_capacity_diving_cylinder",
        () -> new CylinderItem(
            HIGH_CAPACITY_BLOCK.get(),
            new Item.Properties(),
            24000,
            21,
            false
        )
    );

    public static final DeferredHolder<Item, CylinderItem> EMERGENCY = ITEMS.register(
        "emergency_air_cylinder",
        () -> new CylinderItem(
            EMERGENCY_BLOCK.get(),
            new Item.Properties(),
            2000,
            12,
            true
        )
    );

    public static final DeferredHolder<Item, Item> REGULATOR =
        ITEMS.registerSimpleItem("diving_regulator", new Item.Properties().stacksTo(1));

    public static final DeferredHolder<Item, DivingComputerItem> COMPUTER =
        ITEMS.register("diving_computer", () -> new DivingComputerItem(new Item.Properties()));

    public static final DeferredHolder<Item, DivingLampItem> LAMP =
        ITEMS.register("diving_lamp", () -> new DivingLampItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> HELIUM_CARTRIDGE = ITEMS.registerSimpleItem("helium_cartridge");

    public static final DeferredHolder<Item, DivingFinsItem> FINS =
        ITEMS.register("diving_fins", () -> new DivingFinsItem(new Item.Properties()));

    public static final DeferredHolder<Item, SnorkelMaskItem> SNORKEL_MASK =
        ITEMS.register("snorkeling_mask", () -> new SnorkelMaskItem(new Item.Properties()));

    public static final DeferredHolder<Item, DiveKnifeItem> KNIFE =
        ITEMS.register("dive_knife", () -> new DiveKnifeItem(new Item.Properties()));

    public static final DeferredHolder<Block, GuideNodeBlock> GUIDE_NODE = BLOCKS.register(
        "guide_node",
        () -> new GuideNodeBlock(
            BlockBehaviour.Properties.of()
                .strength(1)
                .sound(SoundType.METAL)
                .noOcclusion()
        )
    );

    public static final java.util.EnumMap<DyeColor, DeferredHolder<Item, GuideLineItem>>
            GUIDE_LINES = registerLines();

    private static java.util.EnumMap<DyeColor, DeferredHolder<Item, GuideLineItem>>
            registerLines() {
        ITEMS.registerSimpleBlockItem("guide_node", GUIDE_NODE);

        var result = new java.util.EnumMap<DyeColor, DeferredHolder<Item, GuideLineItem>>(DyeColor.class);

        for (DyeColor color : DyeColor.values()) {
            result.put(
                color,
                ITEMS.register(color.getName() + "_guide_line", () -> new GuideLineItem(new Item.Properties(), color))
            );
        }

        return result;
    }

    public static final DeferredHolder<Block, MachineBlock> COMPRESSOR =
        machine("gas_compressor", MachineBlock.Kind.COMPRESSOR);

    public static final DeferredHolder<Block, MachineBlock> SEPARATOR =
        machine("gas_separator", MachineBlock.Kind.SEPARATOR);

    public static final DeferredHolder<Block, MachineBlock> BLENDER =
        machine("gas_blending_station", MachineBlock.Kind.BLENDER);

    public static final DeferredHolder<Block, MachineBlock> STORAGE = machine("gas_storage", MachineBlock.Kind.STORAGE);

    public static final DeferredHolder<Block, LampLightBlock> LAMP_LIGHT = BLOCKS.register(
        "diving_lamp_light",
        () -> new LampLightBlock(
            BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .replaceable()
                .noLootTable()
                .lightLevel(LightBlock.LIGHT_EMISSION)
                .strength(-1)
                .isViewBlocking((state, level, pos) -> false)
        )
    );

    private static DeferredHolder<Block, CylinderBlock> cylinderBlock(String name) {
        return BLOCKS.register(
            name,
            () -> new CylinderBlock(
                BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()
            )
        );
    }

    private static DeferredHolder<Block, MachineBlock> machine(
        String name,
        MachineBlock.Kind kind
    ) {
        var block = BLOCKS.register(
            name,
            () -> new MachineBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion(),
                kind
            )
        );

        ITEMS.registerSimpleBlockItem(name, block);
        return block;
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasMachine>> MACHINE =
        BLOCK_ENTITIES.register(
            "gas_machine",
            () -> BlockEntityType.Builder.of(
                GasMachine::new,
                COMPRESSOR.get(),
                SEPARATOR.get(),
                BLENDER.get(),
                STORAGE.get()
            )
                .build(null)
        );

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU =
        MENUS.register("gas_machine", () -> IMenuTypeExtension.create(MachineMenu::new));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuideNodeBlockEntity>>
            GUIDE_NODE_ENTITY = BLOCK_ENTITIES.register(
                "guide_node",
                () -> BlockEntityType.Builder.of(GuideNodeBlockEntity::new, GUIDE_NODE.get())
                    .build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CylinderBlockEntity>>
            PLACED_CYLINDER = BLOCK_ENTITIES.register(
                "placed_cylinder",
                () -> BlockEntityType.Builder.of(
                    CylinderBlockEntity::new,
                    ADVANCED_BLOCK.get(),
                    HIGH_CAPACITY_BLOCK.get(),
                    EMERGENCY_BLOCK.get()
                )
                    .build(null)
            );

    public static final DeferredHolder<MenuType<?>, MenuType<ComputerMenu>> COMPUTER_MENU = MENUS.register(
        "diving_computer",
        () -> new MenuType<>(ComputerMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)
    );

    public static final DeferredHolder<DisplaySource, GasDisplaySource> GAS_STATUS =
        DISPLAY_SOURCES.register("gas_status", GasDisplaySource::new);

    static {
        TABS.register(
            "equipment",
            () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.deepdiving"))
                .icon(() -> new ItemStack(COMPUTER.get()))
                .displayItems(
                    (parameters, output) -> {
                        output.accept(COMPRESSOR.get());
                        output.accept(SEPARATOR.get());
                        output.accept(BLENDER.get());
                        output.accept(STORAGE.get());
                        output.accept(HELIUM_CARTRIDGE.get());
                        output.accept(REGULATOR.get());
                        output.accept(ADVANCED.get());
                        output.accept(HIGH_CAPACITY.get());
                        output.accept(EMERGENCY.get());
                        output.accept(COMPUTER.get());
                        output.accept(LAMP.get());
                        output.accept(FINS.get());
                        output.accept(SNORKEL_MASK.get());
                        output.accept(KNIFE.get());
                        output.accept(GUIDE_NODE.get());

                        for (var line : GUIDE_LINES.values()) {
                            output.accept(line.get());
                        }
                    }
                )
                .build()
        );
    }

    public DeepDiving(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        COMPONENTS.register(bus);
        TABS.register(bus);
        DISPLAY_SOURCES.register(bus);
        Gases.register(bus);
        com.diggydwarff.deepdiving.physiology.DivingEffects.REGISTRY.register(bus);
        bus.addListener(DivingPackets::register);
        bus.addListener(this::capabilities);
        bus.addListener(this::setup);
        container.registerConfig(ModConfig.Type.SERVER, DivingConfig.SERVER);
        container.registerConfig(ModConfig.Type.CLIENT, DivingConfig.CLIENT);
        NeoForge.EVENT_BUS.register(DiverService.class);
        NeoForge.EVENT_BUS.register(com.diggydwarff.deepdiving.diving.DepthHoldService.class);
        NeoForge.EVENT_BUS.register(DivingLampService.class);
        NeoForge.EVENT_BUS.register(com.diggydwarff.deepdiving.diving.FinsService.class);
        NeoForge.EVENT_BUS.register(com.diggydwarff.deepdiving.diving.SnorkelService.class);
        NeoForge.EVENT_BUS.register(com.diggydwarff.deepdiving.diving.TreadingService.class);
        NeoForge.EVENT_BUS.register(com.diggydwarff.deepdiving.diving.BreathingBubbleService.class);
        NeoForge.EVENT_BUS.register(com.diggydwarff.deepdiving.sediment.SedimentService.class);
        NeoForge.EVENT_BUS.register(DiveKnifeItem.class);
        NeoForge.EVENT_BUS.register(GuideLineInteraction.class);
    }

    private void capabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            MACHINE.get(),
            (machine, direction) -> machine.itemPort(direction)
        );
        e.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            MACHINE.get(),
            (machine, direction) -> machine.fluidPort(direction)
        );
    }

    private void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(
            () -> {
                DisplaySource.BY_BLOCK_ENTITY.add(MACHINE.get(), GAS_STATUS.get());
                com.diggydwarff.deepdiving.compat.PressureCompatibility.discover();
                com.diggydwarff.deepdiving.compat.DeepSeasCompatibility.discover();
            }
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
