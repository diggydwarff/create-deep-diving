package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;
import com.diggydwarff.deepdiving.gas.Gases;
import com.diggydwarff.deepdiving.machine.GasMachine;
import com.diggydwarff.deepdiving.machine.MachineBlock;
import com.diggydwarff.deepdiving.network.DivingPackets;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(
    modid = DeepDiving.MOD_ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class ClientRegistration {
    public static DivingPackets.Snapshot snapshot;

    @SubscribeEvent
    public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        DepthHoldClient.register(event);
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent e) {
        e.register(DeepDiving.MACHINE_MENU.get(), MachineScreen::new);
        e.register(DeepDiving.COMPUTER_MENU.get(), ComputerScreen::new);
    }

    @SubscribeEvent
    public static void reloadListeners(
        net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e
    ) {
        e.registerReloadListener(
            (net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> {
                DivingPostProcess.reload();
                com.diggydwarff.deepdiving.client.ambience.DeepWaterAmbience.reset();
                com.diggydwarff.deepdiving.client.sediment.SedimentClouds.reset();
                UnderwaterParticleBudget.reset();
                com.diggydwarff.deepdiving.client.lighting.DivingLampLights.reset();
            }
        );
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        com.diggydwarff.deepdiving.diving.FinsService.clientPose = FinsClient::pose;
        DivingPackets.clientReceiver = s -> snapshot = s;
        DivingPackets.poseReceiver = TreadingAnimation::receive;
        com.diggydwarff.deepdiving.network.LampState.clientReceiver =
            com.diggydwarff.deepdiving.client.lighting.DivingLampLights::receive;
        com.diggydwarff.deepdiving.network.BreathingPulse.clientReceiver = EquipmentBreathing::receive;
        com.diggydwarff.deepdiving.network.SedimentPulse.clientReceiver =
            com.diggydwarff.deepdiving.client.sediment.SedimentClouds::receive;
        com.diggydwarff.deepdiving.physiology.DivingEffects.customNauseaVisual =
            entity -> entity == Minecraft.getInstance().player
                && DivingPostProcess.handlesNausea();
        net.createmod.ponder.foundation.PonderIndex.addPlugin(new DivingPonder());
    }

    @SubscribeEvent
    public static void fluids(RegisterClientExtensionsEvent e) {
        DivingEffectIcons.register(e);
        CylinderRenderer.register(e);

        for (Gases.Gas gas : Gases.all()) {
            e.registerFluidType(
                new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.withDefaultNamespace("block/water_still");
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.withDefaultNamespace("block/water_flow");
                    }

                    @Override
                    public int getTintColor() {
                        return gas.tint;
                    }
                },
                gas.type.get()
            );
        }
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(DeepDiving.GUIDE_NODE_ENTITY.get(), GuideNodeRenderer::new);
        e.registerBlockEntityRenderer(DeepDiving.PLACED_CYLINDER.get(), CylinderBlockRenderer::new);
        e.registerBlockEntityRenderer(
            DeepDiving.MACHINE.get(),
            ctx -> new KineticBlockEntityRenderer<GasMachine>(ctx) {
                @Override
                protected BlockState getRenderedBlockState(GasMachine machine) {
                    return shaft(
                        machine.getBlockState()
                            .getValue(MachineBlock.HORIZONTAL_FACING)
                            .getAxis()
                    );
                }

                @Override
                protected net.createmod.catnip.render.SuperByteBuffer getRotatedModel(
                    GasMachine machine,
                    BlockState state
                ) {
                    return net.createmod.catnip.render.CachedBuffers.partialFacing(
                        com.simibubi.create.AllPartialModels.SHAFT_HALF,
                        machine.getBlockState(),
                        machine.getBlockState()
                            .getValue(MachineBlock.HORIZONTAL_FACING)
                            .getOpposite()
                    );
                }

                @Override
                protected void renderSafe(
                    GasMachine machine,
                    float partial,
                    com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.renderer.MultiBufferSource buffers,
                    int light,
                    int overlay
                ) {
                    if (machine.kind() != MachineBlock.Kind.STORAGE) {
                        // This addon has no Flywheel visual factory. Render the shaft even
                        // when Flywheel is active; the base renderer would otherwise skip
                        // it entirely.
                        renderRotatingBuffer(
                            machine,
                            getRotatedModel(machine, getRenderedBlockState(machine)),
                            pose,
                            buffers.getBuffer(net.minecraft.client.renderer.RenderType.solid()),
                            light
                        );
                    }
                }
            }
        );
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers e) {
        CylinderLayer.register(e);
        FinsLayer.register(e);
    }

    @SubscribeEvent
    public static void colors(
        net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event
    ) {
        for (var item : DeepDiving.GUIDE_LINES.values()) {
            event.register(
                (stack, layer) -> 0xFF000000
                    | ((com.diggydwarff.deepdiving.guideline.GuideLineItem) stack.getItem())
                        .color.getTextureDiffuseColor(),
                item.get()
            );
        }
    }

    @SubscribeEvent
    public static void hud(RegisterGuiLayersEvent e) {
        e.registerAboveAll(DeepDiving.id("diving_hud"), ClientRegistration::renderHud);
    }

    private static void renderHud(GuiGraphics g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();

        var s = snapshot;

        if (mc.player == null) {
            return;
        }

        DivingVision.render(g, delta.getGameTimeDeltaPartialTick(false));

        if (s == null) {
            return;
        }

        if (mc.options.hideGui) {
            return;
        }

        if (mc.screen instanceof ComputerScreen) {
            return;
        }

        if (!DivingConfig.HUD.get()
            || !BreathingEquipment.carries(mc.player, DeepDiving.COMPUTER.get())
            || (!s.underwater() && s.ceiling() == 0 && s.warnings() == 0)) {
            return;
        }

        int x = Math.clamp(DivingConfig.HUD_X.get(), 0, Math.max(0, g.guiWidth() - OledDisplay.HUD_WIDTH));

        int y = Math.clamp(DivingConfig.HUD_Y.get(), 0, Math.max(0, g.guiHeight() - OledDisplay.HUD_HEIGHT));

        OledDisplay.hud(g, x, y, s);
    }

    private ClientRegistration() {}
}
