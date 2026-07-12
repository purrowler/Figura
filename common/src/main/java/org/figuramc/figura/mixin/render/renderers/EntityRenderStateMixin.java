package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.figuramc.figura.ducks.FiguraEntityRenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements FiguraEntityRenderStateExtension {

    @Unique
    Integer figura$entityId = null;
    @Unique
    float figura$tickDelta = 1.0f;


    @Override
    public Integer figura$getEntityId() {
        return figura$entityId;
    }

    @Override
    public void figura$setEntityId(Integer id) {
        this.figura$entityId = id;
    }

    @Override
    public float figura$getTickDelta() {
        return figura$tickDelta;
    }

    @Override
    public void figura$setTickDelta(float tickDelta) {
        this.figura$tickDelta = tickDelta;
    }
}
