package org.figuramc.figura.mixin.render.feature;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRendererDispatcherMixin {
    final FiguraFeatureRenderer figuraFeatureRenderer = new FiguraFeatureRenderer();

    @Inject(method = "prepareFrame", at = @At("HEAD"))
    private void figura$renderFiguraFeatures(SubmitNodeStorage submitNodeStorage, CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        SubmitNodeCollection collection = submitNodeStorage.order(0);
        figuraFeatureRenderer.render(collection, submitNodeStorage);
    }
}
