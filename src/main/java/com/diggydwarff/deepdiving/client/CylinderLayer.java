package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.equipment.CylinderItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Follows Create's torso attachment convention for both normal and slim player skins. */
final class CylinderLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    CylinderLayer(PlayerRenderer parent) {
        super(parent);
    }

    static void register(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);

            if (renderer != null) {
                renderer.addLayer(new CylinderLayer(renderer));
            }
        }
    }

    @Override
    public void render(
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        AbstractClientPlayer player,
        float limbSwing,
        float limbAmount,
        float partial,
        float age,
        float yaw,
        float pitch
    ) {
        if (player.isInvisible()
            || player.getPose() == Pose.SLEEPING
            || !(player.getItemBySlot(EquipmentSlot.CHEST).getItem()
                    instanceof CylinderItem cylinder)) {
            return;
        }

        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.translate(0, .43, .48);
        pose.scale(.9f, -.72f, -.8f);
        CylinderRenderer.renderAssembly(cylinder, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
