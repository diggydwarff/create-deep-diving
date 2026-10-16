package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.equipment.CylinderBlock;
import com.diggydwarff.deepdiving.equipment.CylinderBlockEntity;
import com.diggydwarff.deepdiving.equipment.CylinderItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Match the item and worn variants using the installed Create models. */
public final class CylinderBlockRenderer implements BlockEntityRenderer<CylinderBlockEntity> {
    public CylinderBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
        CylinderBlockEntity cylinder,
        float partial,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay
    ) {
        if (!(cylinder.item().getItem() instanceof CylinderItem item)) {
            return;
        }

        pose.pushPose();
        // ItemRenderer centers the Create model before we scale it. Compensate for half its scaled
        // height so every placed tank's base stays on the floor.
        pose.translate(.5, CylinderRenderer.height(item) / 2, .5);
        pose.mulPose(Axis.YP.rotationDegrees(-cylinder.getBlockState().getValue(CylinderBlock.FACING).toYRot() - 180));
        CylinderRenderer.renderAssembly(item, pose, buffers, light, overlay);
        pose.popPose();
    }
}
