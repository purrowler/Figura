package org.figuramc.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractSkullBlock;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraItemStackRenderStateExtension;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelPart;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {

    @Shadow
    protected abstract void renderPlayerArm(PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, float equipProgress, float swingProgress, HumanoidArm arm, PlayerRenderState playerRenderState);

    @Unique Avatar avatar;

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void onRenderHandsWithItems(float tickDelta, PoseStack matrices, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerRenderState, FirstPersonHandsAndItemsRenderState handsState, CallbackInfo ci) {
        avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(avatar);
        FiguraMod.pushProfiler("renderEvent");
        avatar.renderMode = EntityRenderMode.FIRST_PERSON;
        avatar.renderEvent(tickDelta, new FiguraMat4().set(matrices.last().pose()));
        FiguraMod.popProfiler(3);
    }

    @Inject(method = "submitHandsWithItems", at = @At(value = "RETURN"))
    private void afterRenderHandsWithItems(float tickDelta, PoseStack matrices, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerRenderState, FirstPersonHandsAndItemsRenderState handsState, CallbackInfo ci) {
        if (avatar == null)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(avatar);
        FiguraMod.pushProfiler("postRenderEvent");
        avatar.postRenderEvent(tickDelta, new FiguraMat4().set(matrices.last().pose()));
        avatar = null;
        FiguraMod.popProfiler(3);

    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void renderArmWithItem(PlayerRenderState playerRenderState, FirstPersonHandsAndItemsRenderState handsState, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, CallbackInfo ci) {
        if (handsState.isScoping || avatar == null || avatar.luaRuntime == null)
            return;

        boolean main = hand == InteractionHand.MAIN_HAND;
        HumanoidArm mainArm = playerRenderState.avatarRenderState.mainArm;
        HumanoidArm arm = main ? mainArm : mainArm.getOpposite();
        Boolean armVisible = arm == HumanoidArm.LEFT ? avatar.luaRuntime.renderer.renderLeftArm : avatar.luaRuntime.renderer.renderRightArm;

        boolean willRenderItem = !item.isEmpty();
        boolean willRenderArm = (!willRenderItem && main) || item.is(Items.FILLED_MAP) || (!willRenderItem && handsState.mainHandItem.is(Items.FILLED_MAP));

        // hide arm
        if (willRenderArm && !willRenderItem && armVisible != null && !armVisible) {
            ci.cancel();
            return;
        }
        // render arm
        if (!willRenderArm && !playerRenderState.avatarRenderState.isInvisible && armVisible != null && armVisible) {
            matrices.pushPose();
            this.renderPlayerArm(matrices, submitNodeCollector, light, equipProgress, swingProgress, arm, playerRenderState);
            matrices.popPose();
        }

        // hide item
        VanillaModelPart part = arm == HumanoidArm.LEFT ? avatar.luaRuntime.vanilla_model.LEFT_ITEM : avatar.luaRuntime.vanilla_model.RIGHT_ITEM;
        if (willRenderItem && !part.checkVisible()) {
            ci.cancel();
        }
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void renderItem(PlayerRenderState playerRenderState, FirstPersonHandsAndItemsRenderState handsState, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, CallbackInfo ci) {
        if (!(item.getItem() instanceof BlockItem bl) || !(bl.getBlock() instanceof AbstractSkullBlock))
            return;

        Entity entity = AvatarManager.getEntity(playerRenderState.avatarRenderState);
        if (entity != null)
            SkullBlockRendererAccessor.setEntity(entity);

        ItemStackRenderState renderState = hand == InteractionHand.MAIN_HAND ? handsState.mainHandRenderState : handsState.offHandRenderState;
        SkullBlockRendererAccessor.setRenderMode(switch (((FiguraItemStackRenderStateExtension) renderState).figura$getDisplayContext()) {
            case FIRST_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_LEFT_HAND;
            case FIRST_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_RIGHT_HAND;
            case THIRD_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_LEFT_HAND;
            case THIRD_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_RIGHT_HAND;
            default -> playerRenderState.avatarRenderState.mainArm == HumanoidArm.LEFT // should never happen
                    ? SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_LEFT_HAND
                    : SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_RIGHT_HAND;
        });
    }
}
