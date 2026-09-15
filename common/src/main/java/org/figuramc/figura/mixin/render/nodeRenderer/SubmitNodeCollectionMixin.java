package org.figuramc.figura.mixin.render.nodeRenderer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.apache.commons.lang3.function.TriFunction;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FlameSubmitExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraSubmission;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.ducks.NodeCollectorExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements NodeCollectorExtension {
    @Unique
    List<FiguraSubmission> figuraSubmissions = new ArrayList<>();

    @Override
    public <S extends EntityRenderState> void submitFiguraModel(Avatar avatar, S renderState, TriFunction<Avatar, S, SubmitNodeCollector, Void> renderer) {
        figuraSubmissions.add(new FiguraSubmission(avatar, renderState, (TriFunction<Avatar, EntityRenderState, SubmitNodeCollector, Void>) renderer));
    }

    @Override
    public List<FiguraSubmission> getFiguraSubmissions() {
        return figuraSubmissions;
    }

    @WrapOperation(method = "submitFlame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"))
    private void figura$onSubmitFlame(SimpleFeatureRenderPhase instance, SubmitNode node, Operation<Void> original) {
        if (node instanceof FlameFeatureRenderer.Submit flameSubmit) {
            FlameSubmitExtension ext = (FlameSubmitExtension) (Object) flameSubmit;
            Avatar avatar = AvatarManager.getAvatar(flameSubmit.entityRenderState());
            ext.figura$setAvatar(avatar);
        }
        original.call(instance, node);
    }
}
