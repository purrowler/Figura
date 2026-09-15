package org.figuramc.figura.gui;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;

import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.Optional;

public class FiguraGuiRenderer extends PictureInPictureRenderer<FiguraGuiRenderState> {

    private GpuTexture texture;
    private GpuTextureView textureView;
    private GpuTexture depthTexture;
    private GpuTextureView depthTextureView;

    private final ProjectionMatrixBuffer projectionMatrixBuffer = new ProjectionMatrixBuffer(
            "GUI-PIP - " + this.getClass().getSimpleName()
    );
    private final Projection projection = new Projection();

    public FiguraGuiRenderer() {
        super();
    }

    @Override
    public Class<FiguraGuiRenderState> getRenderStateClass() {
        return FiguraGuiRenderState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "Figura GUI";
    }

    @Override
    protected void renderToTexture(FiguraGuiRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector) {
        state.avatar().hudRender(poseStack, submitNodeCollector, state.entity(), state.tickDelta());
    }

    @Override
    public void prepare(FiguraGuiRenderState state, GuiRenderState guiRenderState, net.minecraft.client.renderer.feature.FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
        int pixelW = (state.x1() - state.x0()) * guiScale;
        int pixelH = (state.y1() - state.y0()) * guiScale;
        float guiWidth = (float) (state.x1() - state.x0());
        float guiHeight = (float) (state.y1() - state.y0());

        GpuDevice gpuDevice = RenderSystem.getDevice();
        boolean needsResize = texture == null
                || texture.getWidth(0) != pixelW
                || texture.getHeight(0) != pixelH;

        if (needsResize && texture != null) {
            texture.close();
            texture = null;
            textureView.close();
            textureView = null;
            depthTexture.close();
            depthTexture = null;
            depthTextureView.close();
            depthTextureView = null;
        }

        if (texture == null) {
            texture = gpuDevice.createTexture(
                    () -> "UI Figura GUI texture", 13, GpuFormat.RGBA8_UNORM, pixelW, pixelH, 1, 1
            );
            textureView = gpuDevice.createTextureView(texture);
            depthTexture = gpuDevice.createTexture(
                    () -> "UI Figura GUI depth texture", 9, GpuFormat.D32_FLOAT, pixelW, pixelH, 1, 1
            );
            depthTextureView = gpuDevice.createTextureView(depthTexture);
        }

        gpuDevice.createCommandEncoder().clearColorAndDepthTextures(texture, new org.joml.Vector4f(0f, 0f, 0f, 0f), depthTexture, 0.0);

        projection.setupOrtho(-1000.0F, 1000.0F, guiWidth, guiHeight, true);
        RenderSystem.setProjectionMatrix(
                projectionMatrixBuffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC
        );

        net.minecraft.client.renderer.SubmitNodeStorage sns = ((org.figuramc.figura.mixin.gui.PictureInPictureRendererAccessor) this).figura$getSubmitNodeStorage();
        org.figuramc.figura.model.rendering.FiguraRenderer renderer = state.avatar() == null ? null : state.avatar().renderer;
        if (renderer != null) renderer.beginRender();
        try {
            renderToTexture(state, new PoseStack(), sns);
            try (FeatureRenderDispatcher.PreparedFrame frame = featureRenderDispatcher.prepareFrame(sns);
                 RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                         () -> "Figura GUI", textureView, Optional.empty(), depthTextureView, java.util.OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(renderPass);
                FeatureRenderDispatcher.renderAllFeatures(renderPass, frame);
            }
        } finally {
            if (renderer != null) renderer.endRender();
        }

        blitTexture(state, guiRenderState);
    }

    @Override
    protected void blitTexture(FiguraGuiRenderState state, GuiRenderState guiRenderState) {
        if (textureView == null) return;
        guiRenderState.addBlitToCurrentLayer(
                new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
                        state.pose(),
                        state.x0(), state.y0(), state.x1(), state.y1(),
                        0.0F, 1.0F, 1.0F, 0.0F,
                        -1,
                        state.scissorArea(),
                        null
                )
        );
    }

    @Override
    public void close() {
        if (texture != null) {
            texture.close();
            textureView.close();
            depthTexture.close();
            depthTextureView.close();
        }
        super.close();
    }
}
