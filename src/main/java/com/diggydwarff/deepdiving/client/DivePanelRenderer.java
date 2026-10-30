package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.sign.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.*;

/** Baked plates carry the art; this unticked overlay adds captions, arrows and glow ink. */
public final class DivePanelRenderer implements BlockEntityRenderer<DivePanelBlockEntity> {
    private final Font font;
    private static final Map<DiveSignDesign, ResourceLocation> GLOW =
            new EnumMap<>(DiveSignDesign.class);
    private static final ResourceLocation[] ARROWS = new ResourceLocation[5];

    static {
        for (DiveSignDesign design : DiveSignDesign.values())
            GLOW.put(
                    design,
                    DeepDiving.id(
                            "textures/block/dive_sign/"
                                    + design.getSerializedName()
                                    + "_glow.png"));
        for (int index = 1; index < 5; index++)
            ARROWS[index] = DeepDiving.id("textures/block/dive_sign/arrow_" + index + ".png");
    }

    public DivePanelRenderer(BlockEntityRendererProvider.Context context) {
        font = context.getFont();
    }

    @Override
    public void render(
            DivePanelBlockEntity sign,
            float partial,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        var state = sign.getBlockState();
        var face = state.getValue(DivePanelBlock.FACING);
        poses.pushPose();
        if (state.getValue(DivePanelBlock.POST_MOUNTED))
            poses.translate(-face.getStepX() * 6 / 16.0, 0, -face.getStepZ() * 6 / 16.0);
        poses.translate(.5, .5, .5);
        switch (face) {
            case EAST -> poses.mulPose(Axis.YP.rotationDegrees(-90));
            case SOUTH -> poses.mulPose(Axis.YP.rotationDegrees(-180));
            case WEST -> poses.mulPose(Axis.YP.rotationDegrees(-270));
            case UP -> poses.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> poses.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {}
        }
        poses.translate(0, 0, .4355);
        poses.mulPose(Axis.YP.rotationDegrees(180));
        poses.mulPose(Axis.ZP.rotationDegrees(-90 * state.getValue(DivePanelBlock.ROLL)));
        poses.scale(1f / 128, -1f / 128, 1f / 128);
        int inkLight = sign.glowing() ? LightTexture.FULL_BRIGHT : light;
        if (sign.glowing())
            quad(buffers, poses, GLOW.get(sign.design()), -64, -64, 128, 128, inkLight);
        drawCaption(font, sign.caption(), sign.color(), sign.arrow(), poses, buffers, inkLight);
        poses.popPose();
    }

    public static void drawCaption(
            Font font,
            Component caption,
            int color,
            int arrow,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {
        int width = arrow == 0 ? 104 : 80;
        float scale = 1.05f;
        var lines = font.split(caption, (int) (width / scale));
        while (lines.size() > 2 || (lines.size() * font.lineHeight + 2) * scale > 18) {
            scale *= .9f;
            lines = font.split(caption, Math.max(1, (int) (width / scale)));
        }
        int rgb = DyeColor.byId(color).getTextureDiffuseColor();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int outline = (r * .2126 + g * .7152 + b * .0722) < 90 ? 0xffe9eee1 : 0xff132c36;
        poses.pushPose();
        poses.translate(arrow == 0 ? 0 : -10, 49 - lines.size() * font.lineHeight * scale / 2, .02);
        poses.scale(scale, scale, 1);
        for (int index = 0; index < lines.size(); index++)
            font.drawInBatch8xOutline(
                    lines.get(index),
                    -font.width(lines.get(index)) / 2f,
                    index * font.lineHeight,
                    0xff000000 | rgb,
                    outline,
                    poses.last().pose(),
                    buffers,
                    light);
        poses.popPose();
        if (arrow > 0 && arrow < 5) quad(buffers, poses, ARROWS[arrow], 37, 41, 17, 17, light);
    }

    private static void quad(
            MultiBufferSource buffers,
            PoseStack poses,
            ResourceLocation texture,
            float x,
            float y,
            float width,
            float height,
            int light) {
        var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        vertex(consumer, poses, x, y, 0, 0, light);
        vertex(consumer, poses, x, y + height, 0, 1, light);
        vertex(consumer, poses, x + width, y + height, 1, 1, light);
        vertex(consumer, poses, x + width, y, 1, 0, light);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack poses,
            float x,
            float y,
            float u,
            float v,
            int light) {
        consumer.addVertex(poses.last(), x, y, 0)
                .setColor(0xffffffff)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(poses.last(), 0, 0, 1);
    }
}
