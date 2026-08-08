package org.figuramc.figura.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public final class OutlineCallbackSnapshot {

    private OutlineCallbackSnapshot() {}

    public static Object model;
    public static final List<BiFunction<SubmitNodeCollector, PoseStack, Boolean>> PRE = new ArrayList<>();
    public static final List<Runnable> POST = new ArrayList<>();

    public static void set(Object model, List<BiFunction<SubmitNodeCollector, PoseStack, Boolean>> pre, List<Runnable> post) {
        OutlineCallbackSnapshot.model = model;
        PRE.clear();
        PRE.addAll(pre);
        POST.clear();
        POST.addAll(post);
    }
}
