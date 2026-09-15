package org.figuramc.figura.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Intrinsic
    @Accessor("swingState")
    LivingEntity.SwingState getSwingState();

    @Intrinsic
    @Invoker("getModifiedSwingDuration")
    int getSwingDuration(SwingAnimation animation);

    @Intrinsic
    @Invoker("updateWalkAnimation")
    void invokeUpdateWalkAnimation(float distance);
}
