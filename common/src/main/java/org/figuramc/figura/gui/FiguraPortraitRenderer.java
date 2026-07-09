package org.figuramc.figura.gui;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.util.LightCoordsUtil;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.gui.widgets.permissions.PlayerPermPackElement;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

public class FiguraPortraitRenderer extends PictureInPictureRenderer<FiguraPortraitRenderState> {

    private final ProjectionMatrixBuffer avatarProjectionMatrixBuffer = new ProjectionMatrixBuffer(
            "Portrait-PIP - " + this.getClass().getSimpleName()
    );
    private final Projection avatarProjection = new Projection();
    Map<Avatar, TextureEntry> avatarToTexture = new HashMap<>();
    private boolean renderSkin;
    public FiguraPortraitRenderer() {
        super();
    }

    @Override
    public Class<FiguraPortraitRenderState> getRenderStateClass() {
        return FiguraPortraitRenderState.class;
    }

    @Override
    protected void renderToTexture(FiguraPortraitRenderState portraitState, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);

        Avatar avatar = portraitState.avatar();
        if (avatar != null) {
            renderSkin = !avatar.renderHeadForPortrait(submitNodeCollector, poseStack, LightCoordsUtil.FULL_BRIGHT, portraitState.modelScale(), portraitState.upsideDown());
        } else {
            renderSkin = true;
        }
    }

    @Override
    public void prepare(FiguraPortraitRenderState pictureInPictureRenderState, GuiRenderState guiRenderState, net.minecraft.client.renderer.feature.FeatureRenderDispatcher featureRenderDispatcher, int i) {
        int j = (pictureInPictureRenderState.x1() - pictureInPictureRenderState.x0()) * i;
        int k = (pictureInPictureRenderState.y1() - pictureInPictureRenderState.y0()) * i;
        if (pictureInPictureRenderState.avatar() != null) {
            prepareTexturesAndProjectionForAvatar(pictureInPictureRenderState.avatar(), j, k);
            TextureEntry textureEntry = avatarToTexture.get(pictureInPictureRenderState.avatar());
            RenderSystem.outputColorTextureOverride = textureEntry.textureView;
            RenderSystem.outputDepthTextureOverride = textureEntry.depthTextureView;
            PoseStack poseStack = new PoseStack();
            float translateY = this.getTranslateY(k, i);
            poseStack.translate(j / 2.0F, translateY, 0.0F);
            float f = i * pictureInPictureRenderState.scale();
            poseStack.scale(f, f, -f);
            net.minecraft.client.renderer.SubmitNodeStorage sns = ((org.figuramc.figura.mixin.gui.PictureInPictureRendererAccessor) this).figura$getSubmitNodeStorage();
            org.figuramc.figura.model.rendering.FiguraRenderer renderer = pictureInPictureRenderState.avatar().renderer;
            if (renderer != null) renderer.beginRender();
            try {
                this.renderToTexture(pictureInPictureRenderState, poseStack, sns);
                featureRenderDispatcher.renderAllFeatures(sns);
            } finally {
                if (renderer != null) renderer.endRender();
            }

            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            this.blitTexture(pictureInPictureRenderState, guiRenderState);
        }
        else
            super.prepare(pictureInPictureRenderState, guiRenderState, featureRenderDispatcher, i);
    }

    private void prepareTexturesAndProjectionForAvatar(Avatar avatar, int i, int j) {
        if (avatar == null)
            return;

        TextureEntry entry = avatarToTexture.computeIfAbsent(avatar, k -> new TextureEntry());

        boolean bl = entry.texture == null || entry.texture.getWidth(0) != i || entry.texture.getHeight(0) != j;
        if (entry.texture != null && bl) {
            entry.texture.close();
            entry.texture = null;
            entry.textureView.close();
            entry.textureView = null;
            entry.depthTexture.close();
            entry.depthTexture = null;
            entry.depthTextureView.close();
            entry.sampler.close();
            entry.sampler = null;
        }

        GpuDevice gpuDevice = RenderSystem.getDevice();
        if (entry.texture == null) {
            entry.texture = gpuDevice.createTexture(() -> "UI " + this.getTextureLabel() + " texture " + avatar.name, 13, GpuFormat.RGBA8_UNORM, i, j, 1, 1);
            entry.textureView = gpuDevice.createTextureView(entry.texture);
            entry.depthTexture = gpuDevice.createTexture(() -> "UI " + this.getTextureLabel() + " depth texture " + avatar.name, 9, GpuFormat.D32_FLOAT, i, j, 1, 1);
            entry.depthTextureView = gpuDevice.createTextureView(entry.depthTexture);
            entry.sampler = gpuDevice.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        }

        gpuDevice.createCommandEncoder().clearColorAndDepthTextures(entry.texture, new org.joml.Vector4f(0f, 0f, 0f, 0f), entry.depthTexture, 0.0);
        avatarProjection.setupOrtho(-1000.0F, 1000.0F, i, j, true);
        RenderSystem.setProjectionMatrix(this.avatarProjectionMatrixBuffer.getBuffer(avatarProjection), ProjectionType.ORTHOGRAPHIC);
    }

    @Override
    protected void blitTexture(FiguraPortraitRenderState pictureInPictureRenderState, GuiRenderState guiRenderState) {
        if (!renderSkin){
            TextureEntry entry = avatarToTexture.get(pictureInPictureRenderState.avatar());

            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(entry.textureView, entry.sampler),
                            pictureInPictureRenderState.pose(),
                            pictureInPictureRenderState.x0(),
                            pictureInPictureRenderState.y0(),
                            pictureInPictureRenderState.x1(),
                            pictureInPictureRenderState.y1(),
                            0.0F,
                            1.0F,
                            1.0F,
                            0.0F,
                            -1,
                            pictureInPictureRenderState.scissorArea(),
                            null
                    )
            );
            return;
        }

        if (pictureInPictureRenderState.fallbackSkin() != null) {
            Identifier texture = pictureInPictureRenderState.fallbackSkin();
            // render skin
            GpuTextureView gpuTextureView = Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView();
            GpuSampler sampler = Minecraft.getInstance().getTextureManager().getTexture(texture).getSampler();

            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(gpuTextureView, sampler),
                            pictureInPictureRenderState.pose(),
                            pictureInPictureRenderState.x0(),
                            pictureInPictureRenderState.y0(),
                            pictureInPictureRenderState.x1(),
                            pictureInPictureRenderState.y1(),
                            8/64F,
                            16/64.0F,
                            8/64F,
                            16/64.0F,
                            -1,
                            pictureInPictureRenderState.scissorArea(),
                            null
                    )
            );

            // hat
            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(gpuTextureView, sampler),
                            pictureInPictureRenderState.pose(),
                            pictureInPictureRenderState.x0(),
                            pictureInPictureRenderState.y0(),
                            pictureInPictureRenderState.x1(),
                            pictureInPictureRenderState.y1(),
                            40/64F,
                            48/64.0F,
                            8/64F,
                            16/64.0F,
                            -1,
                            pictureInPictureRenderState.scissorArea(),
                            null
                    )
            );
        } else {
            GpuTextureView gpuTextureView = Minecraft.getInstance().getTextureManager().getTexture(PlayerPermPackElement.UNKNOWN).getTextureView();
            GpuSampler sampler = Minecraft.getInstance().getTextureManager().getTexture(PlayerPermPackElement.UNKNOWN).getSampler();
            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(gpuTextureView, sampler),
                            pictureInPictureRenderState.pose(),
                            pictureInPictureRenderState.x0(),
                            pictureInPictureRenderState.y0(),
                            pictureInPictureRenderState.x1(),
                            pictureInPictureRenderState.y1(),
                            0,
                            1,
                            0,
                            1,
                            -1,
                            pictureInPictureRenderState.scissorArea(),
                            null
                    )
            );
        }
    }

    @Override
    protected String getTextureLabel() {
        return "figura-portrait";
    }

    @Override
    public void close() {
        super.close();
        for (Map.Entry<Avatar, TextureEntry> entry : avatarToTexture.entrySet()) {
            entry.getValue().texture.close();
            entry.getValue().textureView.close();
            entry.getValue().depthTexture.close();
            entry.getValue().depthTextureView.close();
        }
        avatarProjectionMatrixBuffer.close();
    }

    private static final class TextureEntry {
        private GpuTexture texture;
        private GpuTextureView textureView;
        private GpuTexture depthTexture;
        private GpuTextureView depthTextureView;
        private GpuSampler sampler;

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (TextureEntry) obj;
            return Objects.equals(this.texture, that.texture) &&
                    Objects.equals(this.textureView, that.textureView) &&
                    Objects.equals(this.depthTexture, that.depthTexture) &&
                    Objects.equals(this.depthTextureView, that.depthTextureView);
        }

        @Override
        public int hashCode() {
            return Objects.hash(texture, textureView, depthTexture, depthTextureView);
        }

        @Override
        public String toString() {
            return "TextureEntry[" +
                    "texture=" + texture + ", " +
                    "textureView=" + textureView + ", " +
                    "depthTexture=" + depthTexture + ", " +
                    "depthTextureView=" + depthTextureView + ']';
        }


    }

}
