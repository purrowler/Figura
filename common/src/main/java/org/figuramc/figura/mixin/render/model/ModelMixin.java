package org.figuramc.figura.mixin.render.model;
import net.minecraft.client.renderer.SubmitNodeCollector;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Mixin(Model.class)
public class ModelMixin implements FiguraSubmitCallBackExtension {
    @Unique
    private final List<BiFunction<SubmitNodeCollector, PoseStack, Boolean>> figura$preRenderingCallback = new ArrayList<>();
    @Unique
    private final List<Runnable> figura$postRenderingCallback = new ArrayList<>();
    @Unique
    private boolean figura$callbacksDrained;

    @Override
    public void figura$addPreRenderingCallback(BiFunction<SubmitNodeCollector, PoseStack, Boolean> callback) {
        figura$resetIfDrained();
        this.figura$preRenderingCallback.add(callback);
    }

    @Override
    public void figura$addPostRenderingCallback(Runnable callback) {
        figura$resetIfDrained();
        this.figura$postRenderingCallback.add(callback);
    }

    @Override
    public void figura$markCallbacksDrained() {
        this.figura$callbacksDrained = true;
    }

    @Unique
    private void figura$resetIfDrained() {
        if (!figura$callbacksDrained)
            return;
        figura$callbacksDrained = false;
        figura$preRenderingCallback.clear();
        figura$postRenderingCallback.clear();
    }

    @Override
    public List<Runnable> figura$getPostRenderingCallbacks() {
        return figura$postRenderingCallback;
    }

    @Override
    public List<BiFunction<SubmitNodeCollector, PoseStack, Boolean>> figura$getPreRenderingCallbacks() {
        return figura$preRenderingCallback;
    }
}
