package org.figuramc.figura.mixin.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.ActionWheel;
import org.figuramc.figura.gui.PopupMenu;
import org.figuramc.figura.lua.api.keybind.FiguraKeybind;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private double xpos;
    @Shadow private double ypos;

    @Shadow private boolean mouseGrabbed;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onPress(long window, MouseButtonInfo mouseButtonInfo, int action, CallbackInfo ci) {
        if (window != this.minecraft.getWindow().handle())
            return;

        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null || avatar.luaRuntime == null)
            return;

        if (avatar.mousePressEvent(figura$toLuaButton(mouseButtonInfo.button()), action, mouseButtonInfo.modifiers()) && (this.mouseGrabbed || this.minecraft.gui.screen() == null)) {
            ci.cancel();
            return;
        }

        boolean pressed = action != 0;

        if (avatar.luaRuntime != null && FiguraKeybind.set(avatar.luaRuntime.keybinds.keyBindings, InputConstants.Type.MOUSE.getOrCreate(mouseButtonInfo.button()), pressed, mouseButtonInfo.modifiers()))
            ci.cancel();

        if (avatar.luaRuntime != null && pressed && avatar.luaRuntime.host.unlockCursor && this.minecraft.gui.screen() == null)
            ci.cancel();

        if (avatar.luaRuntime != null && pressed && ActionWheel.isEnabled()) {
            int button = mouseButtonInfo.button();
            if (button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT)
                ActionWheel.execute(ActionWheel.getSelected(), button == InputConstants.MOUSE_BUTTON_LEFT);
            ci.cancel();
        }
    }

    @Unique
    private static int figura$toLuaButton(int button) {
        return switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT -> 0;
            case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> 2;
            default -> button - 1;
        };
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double scrollDeltaX, double scrollDeltaY, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar != null && avatar.mouseScrollEvent(scrollDeltaY) && (this.mouseGrabbed || this.minecraft.gui.screen() == null)) {
            ci.cancel();
            return;
        }

        if (ActionWheel.isEnabled()) {
            ActionWheel.scroll(scrollDeltaY);
            ci.cancel();
        } else if (PopupMenu.isEnabled() && PopupMenu.hasEntity()) {
            PopupMenu.scroll(Math.signum(scrollDeltaY));
            ci.cancel();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void onMove(long window, double x, double y, double dx, double dy, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        double moveX = this.mouseGrabbed ? dx : x - this.xpos;
        double moveY = this.mouseGrabbed ? dy : y - this.ypos;
        if (avatar != null && avatar.mouseMoveEvent(moveX, moveY) && (this.mouseGrabbed || this.minecraft.gui.screen() == null)) {
            this.xpos = x;
            this.ypos = y;
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void grabMouse(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (ActionWheel.isEnabled() || (avatar != null && avatar.luaRuntime != null && avatar.luaRuntime.host.unlockCursor))
            ci.cancel();
    }
}
