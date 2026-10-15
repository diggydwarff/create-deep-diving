package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.guideline.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Thin crossed cord strips remain visible from every direction, including unloaded far endpoints.
 */
public final class GuideNodeRenderer implements BlockEntityRenderer<GuideNodeBlockEntity> {
    private static final ResourceLocation CORD = ResourceLocation.withDefaultNamespace("textures/block/white_wool.png");

    public GuideNodeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen(GuideNodeBlockEntity node) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(GuideNodeBlockEntity node) {
        return node.lineBounds();
    }

    @Override
    public void render(
        GuideNodeBlockEntity node,
        float partial,
        PoseStack pose,
        MultiBufferSource buffers,
        int light,
        int overlay
    ) {
        Vec3 origin = Vec3.atLowerCornerOf(node.getBlockPos());

        for (GuideLine line : node.lines()) {
            var other = line.other(node.getBlockPos());

            if (node.getLevel().hasChunkAt(other)
                && node.getLevel().getBlockEntity(other) instanceof GuideNodeBlockEntity
                && node.getBlockPos().asLong() > other.asLong()) {
                continue;
            }

            int color = line.color().getTextureDiffuseColor();

            if (line.glowing()) {
                color = Math.min(255, (color >> 16 & 255) + 24) << 16
                        | Math.min(255, (color >> 8 & 255) + 24) << 8
                        | Math.min(255, (color & 255) + 24);
            }

            int brightness = line.glowing()
                    ? LightTexture.pack(Math.max(12, LightTexture.block(light)), LightTexture.sky(light))
                    : light;

            var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(CORD));

            for (int i = 0; i < 32; i++) {
                Vec3 a = line.point(i / 32.0).subtract(origin);

                Vec3 b = line.point((i + 1) / 32.0).subtract(origin);

                Vec3 direction = b.subtract(a).normalize();

                Vec3 normal = direction.cross(new Vec3(0, 1, 0));

                if (normal.lengthSqr() < .001) {
                    normal = direction.cross(new Vec3(1, 0, 0));
                }

                normal = normal.normalize().scale(.014);
                strip(consumer, pose.last(), a, b, normal, color, brightness, 255);
                strip(consumer, pose.last(), a, b, direction.cross(normal), color, brightness, 255);

                if (line.glowing()) {
                    strip(
                        buffers.getBuffer(RenderType.entityTranslucent(CORD)),
                        pose.last(),
                        a,
                        b,
                        normal.scale(2.1),
                        color,
                        LightTexture.FULL_BRIGHT,
                        32
                    );
                }
            }
        }
    }

    private static void strip(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        Vec3 a,
        Vec3 b,
        Vec3 side,
        int color,
        int light,
        int alpha
    ) {
        vertex(consumer, pose, a.add(side), 0, 0, color, light, alpha);
        vertex(consumer, pose, b.add(side), 0, 1, color, light, alpha);
        vertex(consumer, pose, b.subtract(side), 1, 1, color, light, alpha);
        vertex(consumer, pose, a.subtract(side), 1, 0, color, light, alpha);
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        Vec3 point,
        float u,
        float v,
        int color,
        int light,
        int alpha
    ) {
        consumer.addVertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
            .setColor(color >> 16 & 255, color >> 8 & 255, color & 255, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0, 1, 0);
    }
}
