package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.equipment.CylinderItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Compose the installed Create backtank model itself, so resource-pack replacements also work. */
final class CylinderRenderer extends BlockEntityWithoutLevelRenderer {
    private CylinderRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(
            new IClientItemExtensions() {
                private CylinderRenderer renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new CylinderRenderer();
                    }

                    return renderer;
                }
            },
            DeepDiving.ADVANCED.get(),
            DeepDiving.HIGH_CAPACITY.get(),
            DeepDiving.EMERGENCY.get()
        );
    }

    @Override
    public void renderByItem(
        ItemStack stack,
        ItemDisplayContext context,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay
    ) {
        if (!(stack.getItem() instanceof CylinderItem cylinder)) {
            return;
        }

        pose.pushPose();
        pose.translate(.5, .5, .5); // ItemRenderer already moved the custom model to its block origin.
        renderAssembly(cylinder, pose, buffers, light, overlay);
        pose.popPose();
    }

    static void renderAssembly(
        CylinderItem cylinder,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay
    ) {
        if (cylinder.emergency) {
            tank(pose, buffers, light, overlay, 0, .8f, height(cylinder));
        } else if (cylinder.capacity <= 12000) {
            tank(pose, buffers, light, overlay, 0, 1, height(cylinder));
        } else {
            tank(pose, buffers, light, overlay, -.235, .78f, height(cylinder));
            tank(pose, buffers, light, overlay, .235, .78f, height(cylinder));
            pose.pushPose();
            pose.translate(0, .47, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(90));
            pose.scale(.35f, .65f, .35f);
            render(new ItemStack(AllBlocks.SHAFT.get()), pose, buffers, light, overlay);
            pose.popPose();
        }
    }

    static float height(CylinderItem cylinder) {
        return cylinder.emergency ? .72f : 1.28f;
    }

    private static void tank(
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay,
        double x,
        float width,
        float height
    ) {
        pose.pushPose();
        pose.translate(x, 0, 0);
        pose.scale(width, height, 1);
        render(new ItemStack(AllItems.COPPER_BACKTANK.get()), pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void render(
        ItemStack stack,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay
    ) {
        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(
                stack,
                ItemDisplayContext.NONE,
                light,
                overlay,
                pose,
                buffers,
                Minecraft.getInstance().level,
                0
            );
    }
}
