package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.equipment.CylinderBlockEntity;
import com.diggydwarff.deepdiving.gas.CylinderSupply;
import com.diggydwarff.deepdiving.gas.GasMix;
import com.diggydwarff.deepdiving.gas.Gases;
import com.diggydwarff.deepdiving.machine.GasMachine;
import com.diggydwarff.deepdiving.sign.DivePanelBlockEntity;
import com.diggydwarff.deepdiving.sign.DiveSignDesign;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Small, staged demonstrations with one operation and one readable caption per keyframe. */
public final class DivingPonder implements PonderPlugin {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos BLUE = new BlockPos(1, 1, 2);
    private static final BlockPos GREEN = new BlockPos(3, 1, 2);
    private static final BlockPos TOP = new BlockPos(2, 2, 2);

    @Override
    public String getModId() {
        return DeepDiving.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> h) {
        h.forComponents(DeepDiving.id("gas_compressor"))
                .addStoryBoard("gas_compressor", DivingPonder::compressor);
        h.forComponents(DeepDiving.id("gas_separator"))
                .addStoryBoard("gas_separator", DivingPonder::separator);
        h.forComponents(DeepDiving.id("gas_blending_station"))
                .addStoryBoard("gas_blending_station", DivingPonder::blender);
        h.forComponents(DeepDiving.id("gas_storage"))
                .addStoryBoard("gas_storage", DivingPonder::storage);
        h.forComponents(
                        DeepDiving.id("advanced_diving_cylinder"),
                        DeepDiving.id("high_capacity_diving_cylinder"),
                        DeepDiving.id("emergency_air_cylinder"),
                        DeepDiving.id("diving_computer"))
                .addStoryBoard("diving_equipment", DivingPonder::equipment);
        h.forComponents(
                        DeepDiving.id("depth_map"),
                        DeepDiving.id("map_bundle"),
                        DeepDiving.id("dive_site_sign"),
                        DeepDiving.id(DiveSignDesign.DIVE_SITE.registryName()))
                .addStoryBoard("dive_maps", DivingPonder::charts);
        h.forComponents(DeepDiving.id("dive_log_book"))
                .addStoryBoard("dive_log_book", DivingPonder::logbook);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> h) {
        h.registerTag("deep_diving")
                .title("Deep Diving")
                .description(
                        "Prepare breathing gas, equip a diver, and survey and record your dives.")
                .item(DeepDiving.COMPUTER.get())
                .addToIndex()
                .register();
        var tag = h.addToTag(DeepDiving.id("deep_diving"));
        for (String item :
                new String[] {
                    "gas_compressor",
                    "gas_separator",
                    "gas_blending_station",
                    "gas_storage",
                    "advanced_diving_cylinder",
                    "high_capacity_diving_cylinder",
                    "emergency_air_cylinder",
                    "diving_computer",
                    "depth_map",
                    "map_bundle",
                    "dive_log_book",
                    "dive_site_sign",
                    DiveSignDesign.DIVE_SITE.registryName()
                }) tag.add(DeepDiving.id(item));
    }

    private static void begin(SceneBuilder s, SceneBuildingUtil u, String id, String title) {
        s.title(id, title);
        s.configureBasePlate(0, 0, 5);
        s.showBasePlate();
        s.world().showSection(u.select().position(MACHINE), Direction.DOWN);
        s.idle(20);
    }

    private static void note(SceneBuilder s, SceneBuildingUtil u, String text, BlockPos pos) {
        // Longer instructions receive more reading time; every step can be revisited from the
        // timeline.
        int duration = Math.max(100, Math.min(160, text.length() + 20));
        s.overlay()
                .showText(duration)
                .text(text)
                .pointAt(u.vector().blockSurface(pos, Direction.SOUTH))
                .placeNearTarget()
                .attachKeyFrame();
        s.idle(duration + 10);
    }

    private static void controls(
            SceneBuilder s, SceneBuildingUtil u, BlockPos pos, ItemStack item, boolean sneak) {
        var control =
                s.overlay()
                        .showControls(u.vector().topOf(pos).add(0, .25, 0), Pointing.RIGHT, 100)
                        .rightClick()
                        .withItem(item);
        if (sneak) control.whileSneaking();
    }

    private static void power(CreateSceneBuilder s, SceneBuildingUtil u) {
        s.world().showSection(u.select().position(2, 1, 3), Direction.DOWN);
        s.world().setKineticSpeed(u.select().fromTo(2, 1, 2, 2, 1, 3), 32);
        s.effects().rotationDirectionIndicator(new BlockPos(2, 1, 3));
    }

    private static void highlight(
            SceneBuilder s, SceneBuildingUtil u, BlockPos pos, PonderPalette color) {
        s.overlay().showOutline(color, pos, u.select().position(pos), 100);
    }

    private static ItemStack filledCylinder() {
        ItemStack cylinder = new ItemStack(DeepDiving.ADVANCED.get());
        cylinder.set(
                DeepDiving.SUPPLY.get(),
                new CylinderSupply(GasMix.AIR, DeepDiving.ADVANCED.get().capacity, false));
        return cylinder;
    }

    private static void compressor(SceneBuilder builder, SceneBuildingUtil u) {
        var s = new CreateSceneBuilder(builder);
        begin(s, u, "compressor", "Producing compressed air");
        power(s, u);
        note(
                s,
                u,
                "Power the rear shaft at 16 RPM or faster. More speed produces and fills gas"
                        + " faster.",
                new BlockPos(2, 1, 3));
        highlight(s, u, TOP, PonderPalette.INPUT);
        note(s, u, "Keep the top intake above water. A submerged intake stops compression.", TOP);
        s.world().showSection(u.select().position(BLUE), Direction.WEST);
        highlight(s, u, BLUE, PonderPalette.OUTPUT);
        note(
                s,
                u,
                "Extract compressed air with powered Create pumps and pipes, or attach a gas vessel"
                        + " directly.",
                BLUE);
        controls(s, u, MACHINE, filledCylinder(), false);
        note(
                s,
                u,
                "In the menu, put a cylinder in In. Completed cylinders move to Out for collection"
                        + " or bottom-hopper extraction.",
                MACHINE);
        s.markAsFinished();
    }

    private static void separator(SceneBuilder builder, SceneBuildingUtil u) {
        var s = new CreateSceneBuilder(builder);
        begin(s, u, "separator", "Separating breathing gases");
        power(s, u);
        s.world().showSection(u.select().position(TOP), Direction.DOWN);
        highlight(s, u, TOP, PonderPalette.INPUT);
        note(
                s,
                u,
                "Feed compressed air into the top inlet and power the rear shaft at 16 RPM or"
                        + " faster.",
                TOP);
        s.world().showSection(u.select().position(BLUE), Direction.EAST);
        highlight(s, u, BLUE, PonderPalette.BLUE);
        note(
                s,
                u,
                "Oxygen leaves the blue-marked port. Every 100 mB of air yields 21 mB of oxygen.",
                BLUE);
        s.world().showSection(u.select().position(GREEN), Direction.WEST);
        highlight(s, u, GREEN, PonderPalette.GREEN);
        note(
                s,
                u,
                "Nitrogen leaves the green-marked port: the remaining 79 mB. Keep the two gases in"
                        + " separate vessels.",
                GREEN);
        note(
                s,
                u,
                "Both output buffers need space. Empty storage vessels do not make gas; the"
                        + " separator needs compressed air.",
                MACHINE);
        s.markAsFinished();
    }

    private static void blender(SceneBuilder builder, SceneBuildingUtil u) {
        var s = new CreateSceneBuilder(builder);
        begin(s, u, "blender", "Blending and filling cylinders");
        power(s, u);
        s.world().showSection(u.select().position(BLUE), Direction.EAST);
        highlight(s, u, BLUE, PonderPalette.BLUE);
        note(
                s,
                u,
                "Feed oxygen into the blue-marked port. Power the rear shaft at 16 RPM or faster.",
                BLUE);
        s.world().showSection(u.select().position(GREEN), Direction.WEST);
        highlight(s, u, GREEN, PonderPalette.GREEN);
        note(
                s,
                u,
                "Feed nitrogen into the green-marked port. The selected mixture determines how much"
                        + " is used.",
                GREEN);
        s.world().showSection(u.select().position(TOP), Direction.DOWN);
        highlight(s, u, TOP, PonderPalette.INPUT);
        note(
                s,
                u,
                "Helium enters from the top. Mixtures with no helium do not need a helium supply.",
                TOP);
        controls(s, u, MACHINE, filledCylinder(), false);
        note(
                s,
                u,
                "Use the menu presets, or adjust oxygen and helium in 1% steps. Nitrogen fills the"
                        + " remaining percentage.",
                MACHINE);
        note(
                s,
                u,
                "A different mixture requires an empty cylinder. Purge discards its gas; useful"
                        + " partial input buffers can still fill it.",
                MACHINE);
        note(
                s,
                u,
                "Check the mixture depth range and cylinder pressure rating before diving. A full"
                        + " cylinder can still contain the wrong gas.",
                MACHINE);
        s.markAsFinished();
    }

    private static void storage(SceneBuilder builder, SceneBuildingUtil u) {
        var s = new CreateSceneBuilder(builder);
        begin(s, u, "storage", "Storing industrial gases");
        note(
                s,
                u,
                "Each vessel stores 64,000 mB of one gas. All faces accept Create pipes and pumps.",
                MACHINE);
        s.world()
                .modifyBlockEntity(
                        MACHINE,
                        GasMachine.class,
                        m -> m.tanks[0].setFluid(new FluidStack(Gases.HELIUM.source.get(), 1000)));
        controls(s, u, MACHINE, new ItemStack(DeepDiving.HELIUM_CARTRIDGE.get()), false);
        note(
                s,
                u,
                "Insert a helium cartridge in the menu to add 1,000 mB. A vessel holding another"
                        + " gas rejects it.",
                MACHINE);
        controls(s, u, MACHINE, AllItems.GOGGLES.asStack(), false);
        note(
                s,
                u,
                "Engineer's Goggles show the stored gas and quantity. Display Links can report it"
                        + " to a board.",
                MACHINE);
        controls(s, u, MACHINE, AllItems.WRENCH.asStack(), true);
        note(
                s,
                u,
                "Sneak-wrench pickup preserves the gas and items. Place the vessel again to"
                        + " reconnect it.",
                MACHINE);
        s.markAsFinished();
    }

    private static void equipment(SceneBuilder s, SceneBuildingUtil u) {
        begin(s, u, "equipment", "Preparing for a dive");
        s.world().showSection(u.select().position(3, 1, 2), Direction.DOWN);
        s.world()
                .modifyBlockEntity(
                        new BlockPos(3, 1, 2),
                        CylinderBlockEntity.class,
                        be -> be.setItem(filledCylinder()));
        s.world()
                .createEntity(
                        level -> {
                            var stand = new ArmorStand(level, 1.5, 1, 2.5);
                            stand.setYRot(160);
                            stand.setNoGravity(true);
                            stand.setItemSlot(
                                    EquipmentSlot.HEAD, AllItems.COPPER_DIVING_HELMET.asStack());
                            stand.setItemSlot(EquipmentSlot.CHEST, filledCylinder());
                            return stand;
                        });
        controls(s, u, new BlockPos(1, 2, 2), filledCylinder(), false);
        note(
                s,
                u,
                "Wear a Create diving helmet and equip a filled Deep Diving cylinder in your chest"
                        + " slot.",
                new BlockPos(1, 2, 2));
        s.overlay()
                .showControls(u.vector().topOf(MACHINE).add(0, .25, 0), Pointing.RIGHT, 100)
                .withItem(filledCylinder());
        note(
                s,
                u,
                "The helmet draws breathing gas directly from your cylinder. Only the selected"
                        + " supply is drained.",
                MACHINE);
        controls(s, u, new BlockPos(3, 1, 2), new ItemStack(DeepDiving.EMERGENCY.get()), true);
        note(
                s,
                u,
                "Sneak-use a filled spare in your hand to enable backup. Keep it in your inventory"
                        + " for automatic takeover.",
                new BlockPos(3, 1, 2));
        controls(s, u, MACHINE, new ItemStack(DeepDiving.COMPUTER.get()), false);
        note(
                s,
                u,
                "Carry a computer for the HUD. Use it to inspect gas time, ascent guidance and the"
                        + " recovery guide.",
                MACHINE);
        note(
                s,
                u,
                "Check gas depth range and cylinder pressure rating. Backups supply gas; they do"
                        + " not increase equipment pressure limits.",
                new BlockPos(3, 1, 2));
        s.markAsFinished();
    }

    private static void charts(SceneBuilder s, SceneBuildingUtil u) {
        begin(s, u, "charts", "Surveying with a map bundle");
        s.world().showSection(u.select().fromTo(0, 1, 0, 4, 2, 4), Direction.DOWN);
        var chart =
                s.world()
                        .createItemEntity(
                                u.vector().of(2.5, 2.1, 2.5),
                                Vec3.ZERO,
                                new ItemStack(DeepDiving.DEPTH_MAP.get()));
        controls(s, u, MACHINE, new ItemStack(DeepDiving.DEPTH_MAP.get()), false);
        note(
                s,
                u,
                "Use a new Depth Map once to initialize it. Hold it while exploring water to survey"
                        + " the bottom.",
                MACHINE);
        s.world()
                .modifyEntity(
                        chart,
                        entity ->
                                ((ItemEntity) entity)
                                        .setItem(new ItemStack(DeepDiving.MAP_BUNDLE.get())));
        controls(s, u, MACHINE, new ItemStack(DeepDiving.MAP_BUNDLE.get()), false);
        note(
                s,
                u,
                "Use a Dive Map Bundle to open it. Shift-click initialized depth maps into its 16"
                        + " chart slots.",
                MACHINE);
        controls(s, u, MACHINE, new ItemStack(DeepDiving.MAP_BUNDLE.get()), false);
        note(
                s,
                u,
                "Flip pages with the arrow buttons or Left and Right keys. Only the selected map"
                        + " surveys while the bundle is held.",
                MACHINE);
        var sign = new BlockPos(3, 1, 2);
        s.world()
                .modifyBlockEntity(
                        sign, DivePanelBlockEntity.class, be -> be.setSettings("Reef Camp", 0, 0));
        highlight(s, u, sign, PonderPalette.OUTPUT);
        controls(s, u, sign, new ItemStack(DeepDiving.MAP_BUNDLE.get()), false);
        note(
                s,
                u,
                "Name a Dive Site Sign, then use a map or bundle on it. The site's label is added"
                        + " to the selected map.",
                sign);
        note(
                s,
                u,
                "Take a map out for cartography or an item frame. Its survey, name, scale and"
                        + " markers stay with the same map.",
                MACHINE);
        s.markAsFinished();
    }

    private static void logbook(SceneBuilder s, SceneBuildingUtil u) {
        begin(s, u, "logbook", "Keeping a dive log");
        s.world()
                .createItemEntity(
                        u.vector().of(2.5, 2.1, 2.5),
                        Vec3.ZERO,
                        new ItemStack(DeepDiving.LOG_BOOK.get()));
        controls(s, u, MACHINE, new ItemStack(DeepDiving.COMPUTER.get()), false);
        note(
                s,
                u,
                "Carry a Dive Log Book and a Diving Computer. The book records the computer's"
                        + " readings during a session.",
                MACHINE);
        controls(s, u, MACHINE, new ItemStack(DeepDiving.LOG_BOOK.get()), false);
        note(
                s,
                u,
                "Use the book and choose Start dive. Carry it throughout the dive, then choose End"
                        + " dive when finished.",
                MACHINE);
        note(
                s,
                u,
                "Browse the latest 32 dives with the page arrows. Review depth, gas use, ascent and"
                        + " warning types.",
                MACHINE);
        note(
                s,
                u,
                "Keep the book and computer in your inventory. Removing either interrupts the"
                        + " recording.",
                MACHINE);
        s.markAsFinished();
    }
}
