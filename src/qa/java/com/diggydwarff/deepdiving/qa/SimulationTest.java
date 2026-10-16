package com.diggydwarff.deepdiving.qa;

import com.diggydwarff.deepdiving.client.DiveAlarmState;
import com.diggydwarff.deepdiving.config.PressureProfile;
import com.diggydwarff.deepdiving.gas.CylinderSupply;
import com.diggydwarff.deepdiving.gas.GasMix;
import com.diggydwarff.deepdiving.guideline.GuideLine;
import com.diggydwarff.deepdiving.guideline.GuideNetwork;
import com.diggydwarff.deepdiving.physiology.*;
import com.diggydwarff.deepdiving.presentation.*;
import com.diggydwarff.deepdiving.sediment.SedimentMath;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Deterministic checks; no server, graphics context or optional companion mods required. */
public final class SimulationTest {
    private static final List<String> results = new ArrayList<>();

    private static int failures;

    public static void main(String[] args) throws Exception {
        run("gas percentages reject overflow and invalid fractions", SimulationTest::invalidGas);
        run("gas splitting conserves every unit across all valid mixtures", SimulationTest::gasConservation);
        run("gas codecs reject invalid mixtures and round-trip valid ones", SimulationTest::gasCodecs);
        run("cylinder quantities remain finite, bounded and never negative", SimulationTest::cylinderQuantities);
        run("pressure curves are reversible over their useful range", SimulationTest::pressureCurves);
        run("dimension pressure overrides reject malformed input", SimulationTest::pressureOverrides);
        run("tissues remain at equilibrium at the surface", SimulationTest::surfaceEquilibrium);
        run("recovery multiplier changes unloading without accelerating loading", SimulationTest::recoveryRate);
        run("NDL forecast matches stepping and leaves current tissues untouched", SimulationTest::forecast);
        run("decompression stops use whole three-block steps", SimulationTest::stops);
        run("saved exposure survives while movement holds remain transient", SimulationTest::persistence);
        run("invalid saved physiological values cannot introduce NaN", SimulationTest::invalidPersistence);
        run("pressure protection prevents sudden and catastrophic exposure", SimulationTest::pressureProtection);
        run("first sample cannot become a teleport pressure shock", SimulationTest::initialExposure);
        run("depth hold respects ascent limits and rejects invalid coordinates", SimulationTest::holdMotion);
        run("breath phases have one inhale, one exhale and a quiet interval", SimulationTest::breathCycle);
        run("sound transitions restore exact normal acoustics", SimulationTest::transmission);
        run("depth ambience blends and particle budgets remain bounded", SimulationTest::ambience);
        run("sediment grows, settles and stays bounded", SimulationTest::sediment);
        run("guideline geometry and persistence remain valid", SimulationTest::guidelineGeometry);
        run("duplicate saved guideline IDs cannot corrupt endpoint indexes", SimulationTest::duplicateLines);
        run("orbit preserves additional host camera modes", SimulationTest::orbit);
        run("alarms do not repeat persistent warnings and urgent alerts can interrupt", SimulationTest::alarms);
        run("readouts handle empty supplies and invalid rates", SimulationTest::readouts);

        Path report = Path.of(System.getProperty("deepdiving.qa.report", "build/qa/simulation-results.txt"));
        Files.createDirectories(report.toAbsolutePath().getParent());
        Files.write(report, results);
        System.out.println((results.size() - failures) + "/" + results.size() + " scenario groups passed");

        if (failures != 0) throw new AssertionError(failures + " QA groups failed");
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            results.add("PASS " + name);
        } catch (Throwable failure) {
            failures++;
            results.add("FAIL " + name + ": " + failure);
            failure.printStackTrace();
        }

        System.out.println(results.getLast());
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Unexpected result");
    }

    private static void near(double actual, double expected) {
        check(Math.abs(actual - expected) < 1e-8);
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }

        throw new AssertionError("Invalid input was accepted");
    }

    private static void invalidGas() {
        rejects(() -> new GasMix(Integer.MAX_VALUE, Integer.MAX_VALUE));
        rejects(() -> new GasMix(0, 0));
        rejects(() -> new GasMix(21, -1));
        rejects(() -> new GasMix(50, 51));
        check(new GasMix(100, 0).nitrogen() == 0);
    }

    private static void gasConservation() {
        for (int oxygen = 1; oxygen <= 100; oxygen++) {
            for (int helium = 0; helium <= 100 - oxygen; helium++) {
                GasMix mix = new GasMix(oxygen, helium);

                for (int quantity : new int[]{0, 1, 2, 3, 99, 100, 127, 24000, 1000000}) {
                    int[] split = mix.constituents(quantity);
                    check(Arrays.stream(split).sum() == quantity);
                    check(Arrays.stream(split).allMatch(n -> n >= 0));
                    check(mix.nitrogen() != 0 || split[1] == 0);
                    check(helium != 0 || split[2] == 0);
                }
            }
        }
    }

    private static void gasCodecs() {
        for (GasMix mix : List.of(GasMix.AIR, GasMix.NITROX, GasMix.TRIMIX, GasMix.HELIOX)) {
            var encoded = GasMix.CODEC.encodeStart(JsonOps.INSTANCE, mix).getOrThrow();
            check(GasMix.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(mix));
        }

        check(GasMix.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"oxygen\":80,\"helium\":30}")).error().isPresent());
    }

    private static void cylinderQuantities() {
        var supply = new CylinderSupply(GasMix.TRIMIX, 123.5, true);
        near(supply.consume(200).remaining(), 0);
        check(supply.consume(4).backupEnabled());
        near(supply.consume(-10).remaining(), 123.5);
        rejects(() -> new CylinderSupply(GasMix.AIR, Double.NaN, false));
        rejects(() -> new CylinderSupply(GasMix.AIR, 1000001, false));
        rejects(() -> new CylinderSupply(null, 1, false));
    }

    private static PressureProfile linear() {
        return new PressureProfile(PressureProfile.Mode.LINEAR, 10, 2, 1, null);
    }

    private static void pressureCurves() {
        for (var mode : PressureProfile.Mode.values()) {
            for (double exponent : new double[]{.25, 1, 2, 4}) {
                var curve = new PressureProfile(mode, 100, 6, exponent, null);
                for (double depth : new double[]{0, .05, 1, 10, 80, 150}) {
                    // Tiny curved pressure increments lose a few decimal places
                    // when added to one atmosphere; allow 0.0001 block of error.
                    check(Math.abs(curve.depthAtPressure(curve.pressure(depth)) - depth) < .0001);
                }
            }
        }
    }

    private static void pressureOverrides() {
        check(PressureProfile.validEntry("minecraft:overworld|LINEAR|10|2|1|AUTO"));
        check(PressureProfile.parse("minecraft:overworld|REFERENCE|100|6|2|63").profile().surfaceY() == 63);
        check(!PressureProfile.validEntry("bad|LINEAR|0|2|1|AUTO"));
        check(!PressureProfile.validEntry("minecraft:overworld|LINEAR|NaN|2|1|AUTO"));
    }

    private static void surfaceEquilibrium() {
        var tissues = new TissueModel();
        tissues.step(GasMix.AIR, 1, 36000);
        for (double nitrogen : tissues.nitrogen) near(nitrogen, TissueModel.SURFACE_NITROGEN);
        near(tissues.ceilingPressure(), 1);
    }

    private static void recoveryRate() {
        var ordinary = new TissueModel();
        var quick = new TissueModel();
        ordinary.step(GasMix.AIR, 6, 600, 1);
        quick.step(GasMix.AIR, 6, 600, 4);
        check(Arrays.equals(ordinary.nitrogen, quick.nitrogen));
        ordinary.step(GasMix.AIR, 1, 120, 1);
        quick.step(GasMix.AIR, 1, 120, 4);
        check(quick.maximumLoading() < ordinary.maximumLoading());
    }

    private static void forecast() {
        var model = new TissueModel();
        var stepped = new TissueModel();
        double[] before = model.nitrogen.clone();
        int estimate = model.noDecoSeconds(GasMix.AIR, 6, 1, 1, 1.005);
        int crossing = 3600;

        for (int second = 1; second <= 3600; second++) {
            stepped.step(GasMix.AIR, 6, 1);
            if (stepped.ceilingPressure() > 1.005) {
                crossing = second - 1;
                break;
            }
        }

        check(estimate == crossing && Arrays.equals(before, model.nitrogen));
        check(model.noDecoSeconds(GasMix.AIR, Double.NaN, 1) == 0);
    }

    private static void stops() {
        var model = new TissueModel();
        model.step(GasMix.AIR, 6, 600);
        double stop = DecompressionStops.depth(model, linear());
        check(stop > 0 && stop % 3 == 0);
        check(DecompressionStops.secondsToNextStop(model, linear(), GasMix.AIR, 1, 0, 1, 1, true) == -1);
    }

    private static void persistence() {
        var state = new DiverState();
        state.oxygenation = 45;
        state.tissues.step(GasMix.TRIMIX, 8, 180);
        state.depthHold = true;
        state.finsFloating = true;
        var loaded = DiverState.load(state.save());
        near(loaded.oxygenation, 45);
        check(Arrays.equals(loaded.tissues.nitrogen, state.tissues.nitrogen));
        check(!loaded.depthHold && !loaded.finsFloating && !loaded.initialized);
    }

    private static void invalidPersistence() {
        var tag = new CompoundTag();
        tag.putDouble("oxygenation", Double.NaN);
        tag.putDouble("n0", Double.POSITIVE_INFINITY);
        tag.putDouble("h2", -3);
        var loaded = DiverState.load(tag);
        near(loaded.oxygenation, 100);
        near(loaded.tissues.nitrogen[0], TissueModel.SURFACE_NITROGEN);
        near(loaded.tissues.helium[2], 0);
    }

    private static void pressureProtection() {
        var injury = PressureInjury.step(0, 1, 8, 10, true, true, true, 2, true, 3, 3, 1);
        check(injury.injury() == 0 && injury.shock() == 0 && !injury.catastrophic());
    }

    private static void initialExposure() {
        var injury = PressureInjury.step(0, 1, 8, 1, true, false, true, 2, true, 3, 3, 1);
        check(injury.shock() == 0 && !injury.catastrophic());
        var shock = PressureInjury.step(0, 1, 8, 1, true, true, true, 2, true, 3, 3, 1);
        check(shock.catastrophic());
    }

    private static void holdMotion() {
        check(DepthHoldMotion.vertical(0, 100, .1) <= .1 * .8 / 20);
        check(DepthHoldMotion.vertical(100, 0, 1) >= -.05);
        near(DepthHoldMotion.vertical(Double.NaN, 0, 1), 0);
    }

    private static void breathCycle() {
        for (double effort : new double[]{0, .5, 1}) {
            var cycle = new BreathingCycle();
            cycle.restart(effort, 100);
            int inhale = 0, exhale = 0, quiet = 0, starts = 0, duration = 0;

            do {
                if (cycle.startsPhase()) starts++;
                switch (cycle.phase()) {
                    case BreathingCycle.INHALE -> inhale++;
                    case BreathingCycle.EXHALE -> exhale++;
                    default -> quiet++;
                }
                check(cycle.remainingTicks() >= 0 && cycle.remainingTicks() <= 40);
                duration++;
            } while (!cycle.advance() && duration < 100);

            check(starts == 2 && inhale > 0 && exhale > 0 && quiet > 0 && duration <= 80);
        }
    }

    private static void transmission() {
        near(SoundTransmission.crossing(0, 0), 0);
        near(SoundTransmission.crossing(1, 1), 0);
        near(SoundTransmission.crossing(0, 1), 1);
        float amount = 1;
        for (int tick = 0; tick < 40; tick++) amount = SoundTransmission.approach(amount, 0, .5);
        near(amount, 0);
    }

    private static void ambience() {
        near(AmbienceMath.depthBlend(0, 20, 80), 0);
        near(AmbienceMath.depthBlend(80, 20, 80), 1);
        check(AmbienceMath.particleTarget(900, 10, 1) == 256);
        check(AmbienceMath.cueDelay(true, 0, .5) == Integer.MAX_VALUE);
        check(AmbienceMath.particleLight(0, false) == 0);
    }

    private static void sediment() {
        near(SedimentMath.cloudEnvelope(0, 20), 0);
        near(SedimentMath.cloudEnvelope(20, 20), 0);
        check(SedimentMath.cloudEnvelope(1, 20) > 0);
        near(SedimentMath.cappedDensity(100, 3), 3);
        near(SedimentMath.radialDensity(100, 2), 0);
    }

    private static void guidelineGeometry() {
        var line = new GuideLine(UUID.randomUUID(), BlockPos.ZERO, new BlockPos(10, 0, 0), Direction.UP, Direction.UP, DyeColor.WHITE, false);
        check(GuideLine.load(line.save()).equals(line));
        near(line.point(0).distanceTo(line.start()), 0);
        near(line.point(1).distanceTo(line.end()), 0);
        check(Double.isInfinite(GuideLine.rayDistance(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, new Vec3(1, 0, 0))[0]));
    }

    private static void duplicateLines() {
        UUID id = UUID.randomUUID();
        var first = new GuideLine(id, BlockPos.ZERO, new BlockPos(2, 0, 0), Direction.UP, Direction.UP, DyeColor.WHITE, false);
        var second = new GuideLine(id, new BlockPos(5, 0, 0), new BlockPos(6, 0, 0), Direction.UP, Direction.UP, DyeColor.RED, true);
        var list = new ListTag();
        list.add(first.save());
        list.add(second.save());
        var tag = new CompoundTag();
        tag.put("lines", list);
        var graph = GuideNetwork.load(tag, null);
        check(graph.at(BlockPos.ZERO).equals(List.of(first)));
        check(graph.at(second.a()).isEmpty());
    }

    private static void orbit() {
        var controls = new OrbitControls();
        check(controls.next("rear", "extra", "first", "rear", true, 0, 0).equals("extra"));
        check(!controls.active());
        check(controls.next("extra", "first", "first", "rear", true, 10, 20).equals("rear"));
        check(controls.active());
        controls.zoom(1000);
        near(controls.distance(), 1.5);
        controls.turn(0, 10000);
        near(controls.pitch(), 85);
    }

    private static void alarms() {
        var alarm = new DiveAlarmState();
        check(alarm.update(1, 0, 100, true) == DiveAlarmState.Signal.CAUTION);
        check(alarm.update(1, 200, 100, true) == DiveAlarmState.Signal.NONE);
        check(alarm.update(1 | 128, 201, 100, true) == DiveAlarmState.Signal.URGENT);
    }

    private static void readouts() {
        near(DiveComputerTools.gasSeconds(10, 2), 5);
        near(DiveComputerTools.gasSeconds(0, 2), 0);
        near(DiveComputerTools.gasSeconds(10, 0), -1);
        near(DiveComputerTools.ascentFraction(Double.NaN, 1), 0);
    }

    private SimulationTest() {}
}
