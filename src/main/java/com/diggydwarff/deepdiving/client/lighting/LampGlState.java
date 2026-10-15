package com.diggydwarff.deepdiving.client.lighting;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

/** Raw GL work is bracketed so Minecraft and Iris retain their own state afterwards. */
final class LampGlState implements AutoCloseable {
    private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

    private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

    private final int vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

    private final int vertexBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

    private final int unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);

    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

    private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);

    private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);

    private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);

    private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

    private final int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);

    private final int equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);

    private final boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

    private final int[] viewport = new int[4];

    private final int[] scissor = new int[4];

    private final int[] capabilities = {GL11.GL_DEPTH_TEST, GL11.GL_BLEND, GL11.GL_CULL_FACE,
        GL11.GL_SCISSOR_TEST, GL11.GL_STENCIL_TEST};

    private final boolean[] enabled = new boolean[capabilities.length];

    private final int[] textures = new int[4];

    private final int[] volumes = new int[4];

    private final int[] samplers = new int[4];

    private final ShaderInstance shader = RenderSystem.getShader();

    LampGlState() {
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);

        for (int index = 0; index < capabilities.length; index++) {
            enabled[index] = GL11.glIsEnabled(capabilities[index]);
        }

        for (int unit = 0; unit < textures.length; unit++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            textures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            volumes[unit] = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
            samplers[unit] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        }
        GL13.glActiveTexture(activeTexture);
    }

    @Override
    public void close() {
        for (int unit = 0; unit < textures.length; unit++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[unit]);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, volumes[unit]);
            GL33.glBindSampler(unit, samplers[unit]);
        }
        GL13.glActiveTexture(activeTexture);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GL30.glBindVertexArray(vertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        GL20.glUseProgram(program);
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GL11.glScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
        GL14.glBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
        RenderSystem.depthMask(depthWrite);
        RenderSystem.setShader(() -> shader);

        for (int index = 0; index < capabilities.length; index++) {
            if (enabled[index]) {
                GL11.glEnable(capabilities[index]);
            } else {
                GL11.glDisable(capabilities[index]);
            }
        }
    }
}
