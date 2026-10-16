package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.physiology.DiveDynamics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

@EventBusSubscriber(
    modid = DeepDiving.MOD_ID,
    value = Dist.CLIENT
)
public final class ClientEffects {
    private static int nextHeartbeat;

    private static int nextBreath;

    private static int previousTick;

    private static boolean pressureSoundPlayed;

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        resetSession();
    }

    @SubscribeEvent
    public static void respawn(ClientPlayerNetworkEvent.Clone event) {
        // A new player must not inherit an old blackout or buoyancy target while
        // waiting for the first fresh server snapshot, even in the same level.
        resetSession();
    }

    private static void resetSession() {
        ClientRegistration.snapshot = null;
        DivingPostProcess.reset();
        ComputerAlarms.reset();
        nextBreath = 0;
        nextHeartbeat = 0;
        previousTick = 0;
        pressureSoundPlayed = false;
    }

    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent e) {
        var s = ClientRegistration.snapshot;

        var player = Minecraft.getInstance().player;

        if (s != null
            && player != null
            && DiverService.active(player)
            && !(player.isCreative() && player.getAbilities().flying)) {
            boolean blackout = (s.warnings() & DiverService.BLACKOUT) != 0;

            double scale = DiveDynamics.movementScale(s.oxygenation(), blackout);

            e.getInput().forwardImpulse *= (float) scale;
            e.getInput().leftImpulse *= (float) scale;

            if (blackout) {
                e.getInput().jumping = false;
            }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();

        DivingPostProcess.tick();
        DivingVision.tick();
        ComputerAlarms.tick();

        var s = DivingPostProcess.visualSnapshot();

        if (mc.player == null || !mc.player.isAlive() || s == null) {
            return;
        }

        if (!DivingConfig.VISUAL_EFFECTS.get()
            || !DivingConfig.SOUND_EFFECTS.get()
            || DivingConfig.EFFECT_INTENSITY.get() == 0) {
            return;
        }

        double distress = Math.max(DivingVision.severity(), DivingVision.pressureSeverity());

        int now = mc.player.tickCount;

        if (now < previousTick) {
            nextHeartbeat = nextBreath = now;
        }

        previousTick = now;

        float intensity = DivingConfig.EFFECT_INTENSITY.get().floatValue();

        if (distress > .15 && now >= nextHeartbeat) {
            nextHeartbeat = now + (int) (34 - distress * 17);
            mc.getSoundManager()
                .play(
                    SimpleSoundInstance.forLocalAmbience(
                        SoundEvents.WARDEN_HEARTBEAT,
                        (.08f + (float) distress * .15f) * intensity,
                        .75f + (float) distress * .25f
                    )
                );
        }

        if (s.oxygenation() < 70 && now >= nextBreath) {
            nextBreath = now + (int) (60 - distress * 25);
            mc.getSoundManager()
                .play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PLAYER_HURT_DROWN, .14f * intensity, .65f));
        }

        if (s.pressureShock() > 0 && !pressureSoundPlayed) {
            pressureSoundPlayed = true;
            mc.getSoundManager()
                .play(SimpleSoundInstance.forLocalAmbience(SoundEvents.GLASS_BREAK, .35f * intensity, .45f));
        } else if (s.pressureShock() == 0) {
            pressureSoundPlayed = false;
        }
    }

    @SubscribeEvent
    public static void camera(
        net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles e
    ) {
        if (!DivingConfig.VISUAL_EFFECTS.get()
            || !DivingConfig.CAMERA_EFFECTS.get()
            || DivingConfig.REDUCED_MOTION.get()) {
            return;
        }

        var mc = Minecraft.getInstance();

        var s = DivingPostProcess.visualSnapshot();

        if (mc.player == null || s == null || mc.player.isSpectator()) {
            return;
        }

        double time = (mc.player.tickCount + e.getPartialTick()) / 20;

        double amount = DivingConfig.EFFECT_INTENSITY.get()
                * Math.max(DivingVision.severity(), Math.min(1, s.narcosis() / 3));

        double pressure = DivingVision.pressureSeverity() * DivingConfig.EFFECT_INTENSITY.get();

        e.setRoll(e.getRoll() + (float) (Math.sin(time * 1.15) * amount * 2.2));
        e.setPitch(
            e.getPitch()
                    + (float) (Math.sin(time * .8) * amount * .65
                            + Math.sin(time * 17) * pressure * .35)
        );
    }

    @SubscribeEvent
    public static void sound(PlaySoundEvent e) {
        var s = DivingPostProcess.visualSnapshot();

        var sound = e.getSound();

        if (s == null
            || !DivingConfig.VISUAL_EFFECTS.get()
            || !DivingConfig.SOUND_EFFECTS.get()
            || sound == null
            || s.oxygenation() >= 70 && s.pressureInjury() < 10
            || sound.isLooping()
            || sound instanceof TickableSoundInstance) {
            return;
        }

        if (sound.getSource() == SoundSource.MASTER
            || sound.getSource() == SoundSource.MUSIC
            || sound.getSource() == SoundSource.RECORDS
            || sound.getSource() == SoundSource.PLAYERS) {
            return;
        }

        double distress = Math.max(DivingVision.severity(), DivingVision.pressureSeverity());

        e.setSound(new MuffledSound(sound, (float) (1 - .8 * distress * DivingConfig.EFFECT_INTENSITY.get())));
    }

    private record MuffledSound(SoundInstance original, float factor) implements SoundInstance {
        @Override
        public net.minecraft.resources.ResourceLocation getLocation() {
            return original.getLocation();
        }

        @Override
        public net.minecraft.client.sounds.WeighedSoundEvents resolve(
            net.minecraft.client.sounds.SoundManager manager
        ) {
            return original.resolve(manager);
        }

        @Override
        public net.minecraft.client.resources.sounds.Sound getSound() {
            return original.getSound();
        }

        @Override
        public SoundSource getSource() {
            return original.getSource();
        }

        @Override
        public boolean isLooping() {
            return original.isLooping();
        }

        @Override
        public boolean isRelative() {
            return original.isRelative();
        }

        @Override
        public int getDelay() {
            return original.getDelay();
        }

        @Override
        public float getVolume() {
            return original.getVolume() * factor;
        }

        @Override
        public float getPitch() {
            return original.getPitch() * (.85f + .15f * factor);
        }

        @Override
        public double getX() {
            return original.getX();
        }

        @Override
        public double getY() {
            return original.getY();
        }

        @Override
        public double getZ() {
            return original.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return original.getAttenuation();
        }

        @Override
        public boolean canPlaySound() {
            return original.canPlaySound();
        }

        @Override
        public boolean canStartSilent() {
            return original.canStartSilent();
        }
    }

    private ClientEffects() {}
}
