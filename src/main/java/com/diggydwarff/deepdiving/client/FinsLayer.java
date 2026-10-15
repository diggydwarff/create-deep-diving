package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Rubber pockets, raised ribs, heel straps and small brass buckles follow the animated feet. */
final class FinsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ModelPart pocket;

    private final ModelPart blade;

    private final ModelPart ribs;

    private final ModelPart straps;

    private final ModelPart buckles;

    private static final ResourceLocation RUBBER = finTexture("pocket");

    private static final ResourceLocation TEAL = finTexture("blade");

    private static final ResourceLocation RIBS = finTexture("ribs");

    private static final ResourceLocation STRAPS = finTexture("straps");

    private static final ResourceLocation BRASS =
        ResourceLocation.fromNamespaceAndPath("create", "textures/block/brass_block.png");

    FinsLayer(PlayerRenderer renderer) {
        super(renderer);

        MeshDefinition mesh = new MeshDefinition();

        mesh.getRoot()
            .addOrReplaceChild(
                "pocket",
                CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-2, 9, -2, 4, 3, 4, new CubeDeformation(.08f)),
                PartPose.ZERO
            );
        // Short sections keep every face at the native pixel scale within its 16x16 texture.
        mesh.getRoot()
            .addOrReplaceChild(
                "blade",
                CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-2, 11.2f, -5, 4, .6f, 3)
                    .addBox(-2, 11.2f, -8, 4, .6f, 3)
                    .addBox(-2, 11.2f, -10, 4, .6f, 2),
                PartPose.ZERO
            );

        CubeListBuilder rails = CubeListBuilder.create().texOffs(0, 0);

        for (float x : new float[] {-2, -.3f, 1.4f}) {
            for (float z : new float[] {-6, -10}) {
                rails.addBox(x, 10.9f, z, .6f, .3f, 4);
            }
        }

        mesh.getRoot().addOrReplaceChild("ribs", rails, PartPose.ZERO);
        mesh.getRoot()
            .addOrReplaceChild(
                "straps",
                CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-2.15f, 9.5f, -2.2f, 4.3f, .7f, .8f)
                    .addBox(-2.15f, 9.5f, 1.4f, 4.3f, .7f, .8f),
                PartPose.ZERO
            );
        mesh.getRoot()
            .addOrReplaceChild(
                "buckles",
                CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-2.6f, 9.4f, -.5f, .5f, 1, 1)
                    .addBox(2.1f, 9.4f, -.5f, .5f, 1, 1),
                PartPose.ZERO
            );

        var root = LayerDefinition.create(mesh, 16, 16).bakeRoot();

        pocket = root.getChild("pocket");
        blade = root.getChild("blade");
        ribs = root.getChild("ribs");
        straps = root.getChild("straps");
        buckles = root.getChild("buckles");
    }

    private static ResourceLocation finTexture(String material) {
        return DeepDiving.id("textures/entity/fins_" + material + ".png");
    }

    static void register(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);

            if (renderer != null) {
                renderer.addLayer(new FinsLayer(renderer));
            }
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
        float pitch
    ) {
        if (player.isInvisible()
            || !player.getItemBySlot(EquipmentSlot.FEET).is(DeepDiving.FINS.get())) {
            return;
        }

        for (ModelPart leg :
                new ModelPart[] {getParentModel().leftLeg, getParentModel().rightLeg}) {
            pose.pushPose();
            leg.translateAndRotate(pose);
            render(pocket, RUBBER, pose, buffers, light);
            render(blade, TEAL, pose, buffers, light);
            render(ribs, RIBS, pose, buffers, light);
            render(straps, STRAPS, pose, buffers, light);
            render(buckles, BRASS, pose, buffers, light);
            pose.popPose();
        }
    }

    private static void render(
        ModelPart part,
        ResourceLocation texture,
        PoseStack pose,
        MultiBufferSource buffers,
        int light
    ) {
        part.render(
            pose,
            buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
            light,
            OverlayTexture.NO_OVERLAY
        );
    }
}
