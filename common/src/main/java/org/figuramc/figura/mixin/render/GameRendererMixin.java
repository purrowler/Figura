package org.figuramc.figura.mixin.render;
import net.minecraft.client.renderer.SubmitNodeCollector;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.GameRendererAccessor;
import org.figuramc.figura.gui.FiguraGuiRenderer;
import org.figuramc.figura.gui.FiguraPortraitRenderer;
import org.figuramc.figura.lua.api.ClientAPI;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.utils.RenderUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements GameRendererAccessor {

    @Shadow @Final
    private Minecraft minecraft;

    @Shadow private boolean effectActive;

    @Shadow public abstract void checkEntityPostEffect(Entity entity);

    @Shadow @Final private Camera mainCamera;
    @Shadow @Nullable
    private Identifier postEffectId;

    @Shadow @Final private CrossFrameResourcePool resourcePool;
    @Shadow private float spinningEffectTime;
    @Shadow private float spinningEffectSpeed;
    @Shadow @Final private GuiRenderer guiRenderer;
    @Shadow @Final private GameRenderState gameRenderState;
    @Shadow @Final private net.minecraft.client.renderer.SubmitNodeStorage handAndScreenSubmitNodeStorage;

    @Unique
    private boolean avatarPostShader = false;
    @Unique
    private boolean hasShaders;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        hasShaders = ClientAPI.hasShaderPack();

        CameraRenderState cameraRenderState = this.gameRenderState.levelRenderState.cameraRenderState;
        Matrix4f instance = cameraRenderState.viewRotationMatrix;

        Avatar avatar = AvatarManager.getAvatar(this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity());
        if (!RenderUtils.vanillaModelAndScript(avatar)) {
            return;
        }

        float z = 0f;

        FiguraVec3 rot = avatar.luaRuntime.renderer.cameraRot;
        if (rot != null)
            z = (float) rot.z;

        FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetRot;
        if (offset != null)
            z += (float) offset.z;

        Matrix4f original = new Matrix4f(instance);
        instance.identity();
        instance.rotate(Axis.ZP.rotationDegrees(z));

        FiguraMat4 mat = avatar.luaRuntime.renderer.cameraMat;
        if (mat != null)
            instance.set(mat.toMatrix4f());

        instance.mul(original);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void render(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
        Entity entity = this.minecraft.getCameraEntity();
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (!RenderUtils.vanillaModelAndScript(avatar)) {
            if (avatarPostShader) {
                avatarPostShader = false;
                this.checkEntityPostEffect(entity);
            }
            return;
        }

        Identifier resource = avatar.luaRuntime.renderer.postShader;
        if (resource == null) {
            if (avatarPostShader) {
                avatarPostShader = false;
                this.checkEntityPostEffect(entity);
            }
            return;
        }

        try {
            avatarPostShader = true;
            this.effectActive = true;
            if (this.postEffectId == null || !this.postEffectId.equals(resource)) {
                PostChain postchain = this.minecraft.getShaderManager().getPostChain(resource, LevelTargetBundle.MAIN_TARGETS);
                if (postchain != null)
                    postchain.process(this.minecraft.gameRenderer.mainRenderTarget(), this.resourcePool);
            }
        } catch (Exception ignored) {
            this.effectActive = false;
            avatar.luaRuntime.renderer.postShader = null;
        }
    }

    @Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
    private void checkEntityPostEffect(Entity entity, CallbackInfo ci) {
        if (avatarPostShader)
            ci.cancel();
    }

    @Override @Intrinsic
    public double figura$getFov(Camera camera, float tickDelta, boolean changingFov) {
        return camera.getFov();
    }

    @ModifyArg(method = "<init>", index = 2,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"))
    private List<PictureInPictureRenderer<?>> addPortraitRenderer(List<PictureInPictureRenderer<?>> list) {
        List<PictureInPictureRenderer<?>> newList = new ArrayList<>(list);
        newList.add(new FiguraPortraitRenderer());
        newList.add(new FiguraGuiRenderer());
        return newList;
    }


    @WrapOperation(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;mul(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;", ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V")))
    private Matrix4f figura$moveBobbingToViewMatrix(Matrix4f projectionMatrix, Matrix4fc bobbingMatrix, Operation<Matrix4f> original) {
        Avatar avatar = AvatarManager.getAvatar(this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity());
        if (!RenderUtils.vanillaModelAndScript(avatar) || hasShaders)
            return original.call(projectionMatrix, bobbingMatrix);

        this.gameRenderState.levelRenderState.cameraRenderState.viewRotationMatrix.mulLocal(bobbingMatrix);
        return projectionMatrix;
    }

    @Override
    public GuiRenderer figura$getGuiRenderer() {
        return guiRenderer;
    }

    @Override
    public net.minecraft.client.renderer.SubmitNodeStorage figura$getHandAndScreenSubmitNodeStorage() {
        return handAndScreenSubmitNodeStorage;
    }
}
