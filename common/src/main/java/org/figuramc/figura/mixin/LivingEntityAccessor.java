package org.figuramc.figura.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Intrinsic
    @Invoker("getCurrentSwingDuration")
    int getSwingDuration();

    @Intrinsic
    @Invoker("updateWalkAnimation")
    void invokeUpdateWalkAnimation(float distance);
}
