package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.equipment.SwimsuitTrousersItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

final class SwimsuitLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ModelPart waist, leg;

    private SwimsuitLayer(PlayerRenderer renderer) {
        super(renderer);
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot()
                .addOrReplaceChild(
                        "waist",
                        CubeListBuilder.create()
                                .texOffs(16, 16)
                                .addBox(-4, 9, -2, 8, 3, 4, new CubeDeformation(.13f)),
                        PartPose.ZERO);
        mesh.getRoot()
                .addOrReplaceChild(
                        "leg",
                        CubeListBuilder.create()
                                .texOffs(0, 16)
                                .addBox(-2, 0, -2, 4, 5, 4, new CubeDeformation(.13f)),
                        PartPose.ZERO);
        ModelPart root = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        waist = root.getChild("waist");
        leg = root.getChild("leg");
    }

    static void register(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new SwimsuitLayer(renderer));
        }
    }

    @Override
    public void render(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            AbstractClientPlayer player,
            float limb,
            float amount,
            float partial,
            float age,
            float yaw,
            float pitch) {
        if (player.isInvisible()
                || !(player.getItemBySlot(EquipmentSlot.LEGS).getItem()
                        instanceof SwimsuitTrousersItem item)) return;
        var buffer =
                buffers.getBuffer(
                        RenderType.entityCutoutNoCull(
                                DeepDiving.id(
                                        "textures/entity/swimsuit/"
                                                + item.color.getName()
                                                + ".png")));
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        waist.render(pose, buffer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        for (ModelPart parent :
                new ModelPart[] {getParentModel().leftLeg, getParentModel().rightLeg}) {
            pose.pushPose();
            parent.translateAndRotate(pose);
            leg.render(pose, buffer, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
