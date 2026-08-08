package org.figuramc.figura.mixin.render.nodeRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Mixin(ModelFeatureRenderer.Submit.class)
public class SubmitNodeStorage$ModelSubmitMixin <S> implements FiguraSubmitCallBackExtension {

    @Unique
    private final List<BiFunction<SubmitNodeCollector, PoseStack, Boolean>> figura$preRenderingCallback = new ArrayList<>();
    @Unique
    private final List<Runnable> figura$postRenderingCallback = new ArrayList<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void figura$snapshotModelCallbacks(RenderType renderType, Pose pose, Model<? super S> model, S state, int lightCoords, int overlayCoords, int tintedColor, TextureAtlasSprite sprite, Pose sheetedDecalPose, CallbackInfo ci) {
        FiguraSubmitCallBackExtension modelExtension = (FiguraSubmitCallBackExtension) model;
        figura$preRenderingCallback.addAll(modelExtension.figura$getPreRenderingCallbacks());
        figura$postRenderingCallback.addAll(modelExtension.figura$getPostRenderingCallbacks());
        modelExtension.figura$markCallbacksDrained();
    }

    @Override
    public void figura$addPreRenderingCallback(BiFunction<SubmitNodeCollector, PoseStack, Boolean> callback) {
        this.figura$preRenderingCallback.add(callback);
    }

    @Override
    public void figura$addPostRenderingCallback(Runnable callback) {
        this.figura$postRenderingCallback.add(callback);
    }

    @Override
    public List<BiFunction<SubmitNodeCollector, PoseStack, Boolean>> figura$getPreRenderingCallbacks() {
        return figura$preRenderingCallback;
    }

    @Override
    public List<Runnable> figura$getPostRenderingCallbacks() {
        return figura$postRenderingCallback;
    }
}
