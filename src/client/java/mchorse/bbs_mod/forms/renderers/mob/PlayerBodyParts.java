package mchorse.bbs_mod.forms.renderers.mob;

import mchorse.bbs_mod.forms.forms.MobForm;
import net.minecraft.client.model.ModelPart;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves the six independently controllable sections of a vanilla player model.
 *
 * <p>This deliberately resolves existing {@link ModelPart} instances from the MobRig. It does
 * not replace, clone, or mutate the player's model, so the skin, geometry, and texture remain
 * owned by the normal BBS/Minecraft renderer.</p>
 *
 * <p>The returned parts are the model's original parts. Callers must apply temporary render
 * transforms and restore them after rendering, following the same rule as MobPoseApplier.</p>
 */
public final class PlayerBodyParts
{
    public enum Part
    {
        HEAD("head", "head"),
        TORSO("body", "body", "torso", "chest"),
        LEFT_ARM("left_arm", "left_arm", "leftArm"),
        RIGHT_ARM("right_arm", "right_arm", "rightArm"),
        LEFT_LEG("left_leg", "left_leg", "leftLeg"),
        RIGHT_LEG("right_leg", "right_leg", "rightLeg");

        public final String id;
        private final String[] boneNames;

        Part(String id, String... boneNames)
        {
            this.id = id;
            this.boneNames = boneNames;
        }

        private ModelPart resolve(MobRig rig)
        {
            for (String name : this.boneNames)
            {
                ModelPart part = rig.part(name);

                if (part != null)
                {
                    return part;
                }
            }

            return null;
        }
    }

    private PlayerBodyParts()
    {}

    /**
     * Resolve the six vanilla player sections, or an empty map for non-player forms.
     * Missing model parts are omitted, allowing callers to detect unsupported/custom rigs.
     */
    public static Map<Part, ModelPart> resolve(MobForm form, MobRig rig)
    {
        if (form == null || rig == null || !form.isPlayer())
        {
            return Collections.emptyMap();
        }

        EnumMap<Part, ModelPart> parts = new EnumMap<>(Part.class);

        for (Part part : Part.values())
        {
            ModelPart modelPart = part.resolve(rig);

            if (modelPart != null)
            {
                parts.put(part, modelPart);
            }
        }

        return Collections.unmodifiableMap(parts);
    }

    /** True only when all six expected vanilla player sections were found. */
    public static boolean isComplete(Map<Part, ModelPart> parts)
    {
        return parts != null && parts.size() == Part.values().length;
    }
}
