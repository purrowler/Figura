package org.figuramc.figura.utils;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a single ModelPart for submission, using a pose snapshot as its render state.
 * <p>
 * Vanilla's {@code submitModelPart} wraps the part in a {@code Model.Simple} whose setupAnim is a
 * no-op, so the deferred draw renders the part's <i>live</i> pose. Armor models are shared between
 * entities, so by draw time another entity's setupAnim may have re-posed the part (visible as e.g.
 * armor trims mirroring another player's animations). Restoring the snapshot in setupAnim makes
 * the draw use the pose the part had when it was submitted, like vanilla model+state submissions.
 */
public class PosedModelPartModel extends Model<List<PartPose>> {

    public PosedModelPartModel(ModelPart part) {
        // the render type function is unused: submissions always pass an explicit RenderType
        super(part, RenderTypes::entityCutout);
    }

    public List<PartPose> snapshotPose() {
        List<ModelPart> parts = this.allParts();
        List<PartPose> poses = new ArrayList<>(parts.size());
        for (ModelPart part : parts)
            poses.add(part.storePose()); // includes visibility, via ModelPartMixin's PartPose extension
        return poses;
    }

    @Override
    public void setupAnim(List<PartPose> poses) {
        List<ModelPart> parts = this.allParts();
        for (int i = 0; i < poses.size(); i++)
            parts.get(i).loadPose(poses.get(i));
    }
}
