package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Functional computer audio is independent of the optional distress presentation. */
public final class ComputerAlarms {
    private static DiveAlarmState alarms = new DiveAlarmState();

    private static Object player;

    private static Object level;

    private static Object ignoredSnapshot;

    private static long clock;

    public static void reset() {
        alarms = new DiveAlarmState();
        ignoredSnapshot = null;
        level = null;
        player = null;
        clock = 0;
    }

    static void tick() {
        var mc = Minecraft.getInstance();

        var s = ClientRegistration.snapshot;

        if (mc.player == null || !mc.player.isAlive()) {
            if (player != null) {
                reset();
                ignoredSnapshot = s;
            }

            return;
        }

        if (player != mc.player || level != mc.level) {
            if (player != null) {
                ignoredSnapshot = s;
            }

            alarms = new DiveAlarmState();
            player = mc.player;
            level = mc.level;
        }

        if (mc.isPaused() || s == null || s == ignoredSnapshot) {
            return;
        }

        boolean audible = DivingConfig.COMPUTER_ALARMS.get()
            && DivingConfig.ALARM_VOLUME.get() > 0
            && DiverService.active(mc.player)
            && BreathingEquipment.carries(mc.player, DeepDiving.COMPUTER.get());

        var signal = alarms.update(
            s.warnings(),
            ++clock,
            DivingConfig.ALARM_COOLDOWN.get() * 20,
            audible
        );

        if (signal != DiveAlarmState.Signal.NONE) {
            mc.getSoundManager()
                .play(
                    SimpleSoundInstance.forLocalAmbience(
                        SoundEvents.NOTE_BLOCK_PLING.value(),
                        DivingConfig.ALARM_VOLUME.get().floatValue(),
                        signal == DiveAlarmState.Signal.URGENT ? 1.15f : .8f
                    )
                );
        }
    }

    private ComputerAlarms() {}
}
