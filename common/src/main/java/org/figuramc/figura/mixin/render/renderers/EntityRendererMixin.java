package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.mixin.EntityAccessor;
import org.figuramc.figura.ducks.CameraRenderStateExtension;
import org.figuramc.figura.ducks.EntityRendererAccessor;
import org.figuramc.figura.ducks.FiguraEntityRenderStateExtension;
import org.figuramc.figura.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> implements EntityRendererAccessor {

    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double d, double e, double f, CallbackInfoReturnable<Boolean> cir) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (avatar != null && avatar.permissions.get(Permissions.OFFSCREEN_RENDERING) == 1)
            cir.setReturnValue(true);
    }

    @Inject(at = @At("HEAD"), method = "extractRenderState")
    private void extractRenderState(T entity, S entityRenderState, float f, CallbackInfo ci) {
        int rawId = ((EntityAccessor) entity).figura$getRawId();
        ((FiguraEntityRenderStateExtension)entityRenderState).figura$setEntityId(rawId == 0 ? null : rawId);
        ((FiguraEntityRenderStateExtension)entityRenderState).figura$setTickDelta(f);
    }


    @ModifyArg(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V"), index = 6)
    private CameraRenderState setAvatarForSubmission(CameraRenderState cameraRenderState, @Local(argsOnly = true) S entityRenderState) {
        Avatar figura$avatar = AvatarManager.getAvatar(entityRenderState);

        if (figura$avatar != null) {
            CameraRenderState replacement = new CameraRenderState();
            replacement.pos = cameraRenderState.pos;
            replacement.blockPos = cameraRenderState.blockPos;
            replacement.initialized = cameraRenderState.initialized;
            replacement.orientation = cameraRenderState.orientation;

            // i literally cannot believe we have to do this, but here we are
            ((CameraRenderStateExtension)replacement).figura$setAvatar(figura$avatar);
            ((CameraRenderStateExtension)replacement).figura$setRenderingNameTag(figura$isRenderingName());
            return replacement;
        }
        return cameraRenderState;
    }
}
