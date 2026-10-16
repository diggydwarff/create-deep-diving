package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class DivingPonder implements PonderPlugin {
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
            DeepDiving.id("diving_regulator"),
            DeepDiving.id("diving_computer")
        )
            .addStoryBoard("diving_equipment", DivingPonder::equipment);
    }

    private static void begin(
        SceneBuilder s,
        SceneBuildingUtil u,
        String id,
        String title
    ) {
        s.title(id, title);
        s.configureBasePlate(0, 0, 5);
        s.showBasePlate();
        s.world().showSection(u.select().fromTo(0, 1, 0, 4, 3, 4), Direction.DOWN);
        s.idle(20);
    }

    private static void say(
        SceneBuilder s,
        SceneBuildingUtil u,
        String text,
        int x,
        int y,
        int z
    ) {
        s.overlay().showText(80).text(text).pointAt(u.vector().centerOf(x, y, z)).placeNearTarget();
        s.idle(90);
    }

    private static void compressor(SceneBuilder s, SceneBuildingUtil u) {
        begin(s, u, "compressor", "Producing compressed air");
        say(
            s,
            u,
            "Supply at least 16 RPM through the rear shaft. More speed produces and fills gas faster.",
            2,
            1,
            3
        );
        say(
            s,
            u,
            "Keep the top intake above water. Use Create pipes and pumps to extract compressed air.",
            2,
            2,
            2
        );
        say(
            s,
            u,
            "Insert a cylinder into the input slot. Filled cylinders leave through the output slot; a"
                    + " bottom hopper can extract them.",
            2,
            1,
            2
        );
        s.markAsFinished();
    }

    private static void separator(SceneBuilder s, SceneBuildingUtil u) {
        begin(s, u, "separator", "Separating breathing gases");
        say(s, u, "Feed compressed air into the top inlet and power the rear shaft.", 2, 2, 2);
        say(
            s,
            u,
            "From the front, oxygen exits left and nitrogen exits right. Each 100 mB batch becomes 21"
                    + " mB oxygen and 79 mB nitrogen.",
            1,
            1,
            2
        );
        say(
            s,
            u,
            "Both outputs need free space. Store the gases in separate Create tanks or gas storage"
                    + " vessels.",
            3,
            1,
            2
        );
        s.markAsFinished();
    }

    private static void blender(SceneBuilder s, SceneBuildingUtil u) {
        begin(s, u, "blender", "Blending and filling cylinders");
        say(
            s,
            u,
            "From the front, feed oxygen from the left, nitrogen from the right, and helium from the"
                    + " top.",
            2,
            2,
            2
        );
        say(
            s,
            u,
            "Select Air, Nitrox, Trimix or Heliox in the menu. Oxygen and helium can also be adjusted"
                    + " in 1% steps.",
            2,
            1,
            2
        );
        say(
            s,
            u,
            "A cylinder containing a different mixture must be purged first. Filling only consumes gas"
                    + " when every required constituent is available.",
            2,
            1,
            2
        );
        say(
            s,
            u,
            "Wear the filled cylinder and Create diving helmet. Carry a regulator and computer; enable"
                    + " inventory backups with sneak-use.",
            2,
            1,
            2
        );
        s.markAsFinished();
    }

    private static void storage(SceneBuilder s, SceneBuildingUtil u) {
        begin(s, u, "storage", "Storing industrial gases");
        say(
            s,
            u,
            "Each vessel holds up to 64000 mB of one gas. All faces support Create pipes and pumps.",
            2,
            1,
            2
        );
        say(
            s,
            u,
            "Insert expensive helium cartridges to release 1000 mB helium each. A vessel containing"
                    + " another gas will reject the cartridge.",
            2,
            1,
            2
        );
        say(
            s,
            u,
            "Engineer's Goggles and Display Links report gas quantities. Sneak-wrench pickup preserves"
                    + " the gas and inventory.",
            2,
            1,
            2
        );
        s.markAsFinished();
    }

    private static void equipment(SceneBuilder s, SceneBuildingUtil u) {
        begin(s, u, "equipment", "Preparing for a dive");
        s.world()
            .createEntity(
                level -> {
                    var stand = new net.minecraft.world.entity.decoration.ArmorStand(
                        level,
                        2.5,
                        1,
                        2.5
                    );

                    stand.setYRot(160);
                    stand.setNoGravity(true);
                    stand.setItemSlot(
                        net.minecraft.world.entity.EquipmentSlot.HEAD,
                        com.simibubi.create.AllItems.COPPER_DIVING_HELMET.asStack()
                    );
                    stand.setItemSlot(
                        net.minecraft.world.entity.EquipmentSlot.CHEST,
                        com.simibubi.create.AllItems.COPPER_BACKTANK.asStack()
                    );
                    return stand;
                }
            );
        s.overlay()
            .showControls(u.vector().of(2.5, 3, 2.5), net.createmod.catnip.math.Pointing.RIGHT, 80)
            .withItem(com.simibubi.create.AllItems.COPPER_DIVING_HELMET.asStack());
        say(s, u, "Wear a Create diving helmet and a filled cylinder in your chest slot.", 2, 2, 2);
        s.addKeyframe();
        s.overlay()
            .showControls(u.vector().of(2.5, 2, 2.5), net.createmod.catnip.math.Pointing.RIGHT, 80)
            .withItem(new net.minecraft.world.item.ItemStack(DeepDiving.REGULATOR.get()));
        say(
            s,
            u,
            "Carry a regulator in your inventory. The helmet and cylinder need it to supply gas.",
            2,
            1,
            2
        );
        s.addKeyframe();
        s.overlay()
            .showControls(u.vector().of(3.5, 1.5, 2.5), net.createmod.catnip.math.Pointing.RIGHT, 80)
            .rightClick()
            .whileSneaking()
            .withItem(new net.minecraft.world.item.ItemStack(DeepDiving.EMERGENCY.get()));
        say(
            s,
            u,
            "Sneak-use a filled spare to enable backup. It takes over when your worn supply empties.",
            3,
            1,
            2
        );
        s.addKeyframe();
        s.overlay()
            .showControls(u.vector().of(2.5, 2, 2.5), net.createmod.catnip.math.Pointing.RIGHT, 80)
            .rightClick()
            .withItem(new net.minecraft.world.item.ItemStack(DeepDiving.COMPUTER.get()));
        say(
            s,
            u,
            "Carry a computer for the HUD; use it for gas time, ascent guidance and recovery help.",
            2,
            1,
            2
        );
        s.addKeyframe();
        say(
            s,
            u,
            "Check both the cylinder pressure rating and gas depth range. A spare supplies gas, but"
                    + " does not protect your body.",
            2,
            2,
            2
        );
        s.markAsFinished();
    }
}
