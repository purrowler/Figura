package org.figuramc.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraSkullRenderStateExtension;
import org.figuramc.figura.ducks.PlayerHeadRenderInfoExtension;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.figuramc.figura.ducks.SkullBlockRendererHelper;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.world.BlockStateAPI;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin implements BlockEntityRenderer<SkullBlockEntity, SkullBlockRenderState>, PlayerHeadRenderInfoExtension {

    @Unique
    private static Avatar avatar;
    @Unique
    private static SkullBlockRenderState block;

    @Inject(at = @At("HEAD"), method = "submitSkull", cancellable = true)
    private static void renderSkull(float yaw, PoseStack stack, SubmitNodeCollector submitNodeCollector, int light, SkullModelBase model, RenderType renderLayer, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        // parse block and items first, so we can yeet them in case of a missed event
        if (avatar == null) {
            avatar = SkullBlockRendererHelper.getAvatar();
            SkullBlockRendererHelper.setAvatar(null);
        }

        SkullBlockRenderState localBlock = block;
        block = null;

        ItemStack localItem = SkullBlockRendererAccessor.getItem();
        SkullBlockRendererAccessor.setItem(null);

        Entity localEntity = SkullBlockRendererAccessor.getEntity();
        SkullBlockRendererAccessor.setEntity(null);

        SkullBlockRendererAccessor.SkullRenderMode localMode = SkullBlockRendererAccessor.getRenderMode();
        SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.OTHER);

        ItemDisplayContext localDisplayContext = SkullBlockRendererAccessor.getDisplayContext();
        SkullBlockRendererAccessor.setDisplayContext(null);

        // avatar pointer incase avatar variable is set during render. (unlikely)
        Avatar localAvatar = avatar;
        avatar = null;

        if (localAvatar == null || localAvatar.permissions.get(Permissions.CUSTOM_SKULL) == 0)
            return;

        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(localAvatar);
        FiguraMod.pushProfiler("skullRender");

        // event
        BlockStateAPI b = localBlock == null ? null : new BlockStateAPI(localBlock.blockState, localBlock.blockPos);
        ItemStackAPI i = localItem != null ? ItemStackAPI.verify(localItem) : null;
        EntityAPI<?> e = localEntity != null ? EntityAPI.wrap(localEntity) : null;
        String m = localMode.name();

        FiguraMod.pushProfiler(localBlock != null ? localBlock.blockPos.toString() : String.valueOf(i));

        FiguraMod.pushProfiler("event");
        boolean bool = localAvatar.skullRenderEvent(tickDelta, b, i, e, m);

        // render skull :3
        FiguraMod.popPushProfiler("render");
        int prevComplexity = localAvatar.complexity.remaining;
        localAvatar.complexity.remaining = localAvatar.permissions.get(Permissions.COMPLEXITY);
        boolean rendered = !bool && localAvatar.skullRender(stack, submitNodeCollector, light, null, yaw);
        localAvatar.complexity.remaining = prevComplexity;

        FiguraMod.popProfiler(5);

        if (bool || rendered)
            ci.cancel();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;submitSkull(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"), method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    public void render(SkullBlockRenderState skullBlockRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        block = skullBlockRenderState;
        avatar = ((FiguraSkullRenderStateExtension) skullBlockRenderState).figura$getAvatar();
        SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.BLOCK);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        Avatar localAvatar = avatar; // avatar pointer incase avatar variable is set during render.
        return localAvatar == null || localAvatar.permissions == null ? BlockEntityRenderer.super.shouldRenderOffScreen() : localAvatar.permissions.get(Permissions.OFFSCREEN_RENDERING) == 1;
    }

    @Inject(at = @At("TAIL"), method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    private void storeAvatarInState(SkullBlockEntity blockEntity, SkullBlockRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci) {
        if (state.skullType == SkullBlock.Types.PLAYER) {
            ResolvableProfile profile = blockEntity.getOwnerProfile();
            Avatar stateAvatar = profile != null ? AvatarManager.getAvatarForPlayer(profile.partialProfile().id()) : null;
            ((FiguraSkullRenderStateExtension) state).figura$setAvatar(stateAvatar);
        } else {
            ((FiguraSkullRenderStateExtension) state).figura$setAvatar(null);
        }
    }
}
