package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.forms.renderers.mob.MobRig;
import mchorse.bbs_mod.forms.renderers.mob.PlayerBodyParts;
import mchorse.bbs_mod.forms.renderers.mob.PlayerBodyParts.Part;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIMobEditor;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.values.UIValues;

public class UIMobFormPanel extends UIFormPanel<MobForm>
{
    public UIButton pick;
    public UIModelPoseEditor poseEditor;
    public UIMobEditor mob;
    public UIModelPoseEditor disassemblyEditor;
    public UISection disassemblySection;

    public UIMobFormPanel(UIForm editor)
    {
        super(editor);

        this.pick = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, (b) ->
        {
            Link link = this.form.texture.get();

            UITexturePicker.open(this.getContext(), link, (l) -> this.form.texture.set(l));
        });
        UIToggle slim = UIValues.toggle(UIKeys.FORMS_EDITOR_SLIM, () -> this.form.slim);

        slim.tooltip(UIKeys.FORMS_EDITOR_SLIM_TOOLTIP);

        this.poseEditor = new UIModelPoseEditor();
        this.poseEditor.transform.barBackground();

        this.disassemblyEditor = new UIModelPoseEditor();
        this.disassemblyEditor.transform.barBackground();
        this.disassemblySection = new UISection(IKey.constant("Player Body Disassembler"));
        this.disassemblySection.fields.add(
            new UIButton(IKey.constant("Explode body"), (b) -> this.explodeBody()),
            new UIButton(IKey.constant("Attach / Reset all"), (b) -> this.resetDisassembly()),
            this.disassemblyEditor
        );

        this.mob = new UIMobEditor((id, nbt) ->
        {
            this.form.mobID.set(id);
            this.form.mobNBT.set(nbt);
        });
        /* Another mob is another skeleton: the bone list and the pose group are only right
         * again once the whole panel is refilled, which is what startEdit does. */
        this.mob.onClose(() -> this.editor.startEdit(this.form));

        this.options.add(UI.label(UIKeys.FORMS_EDITORS_MOB_ID), this.mob, this.pick, slim, this.poseEditor, this.disassemblySection);
    }

    @Override
    public void startEdit(MobForm form)
    {
        super.startEdit(form);

        this.mob.set(this.form.mobID.get(), this.form.mobNBT.get());

        MobRig rig = MobFormRenderer.getRig(this.form);

        this.poseEditor.setValuePose(form.pose);
        /* The mob id is the pose group, so a chicken's presets don't show up under a zombie. */
        this.poseEditor.setPose(form.pose.get(), this.form.mobID.get());
        /* No flipped-parts table: vanilla part names are already left_/right_, which is exactly
         * what Pose's own mirror rule matches. */
        this.poseEditor.fillGroups(rig, null, true, null);

        this.disassemblySection.setVisible(form.isPlayer());
        this.disassemblyEditor.setValuePose(form.disassemblyPose);
        this.disassemblyEditor.setPose(form.disassemblyPose.get(), "player_disassembly");
        this.disassemblyEditor.fillGroups(rig, null, true, null);

        this.options.resize();
    }

    private void explodeBody()
    {
        if (this.form == null || !this.form.isPlayer()) return;

        MobRig rig = MobFormRenderer.getRig(this.form);
        java.util.Map<Part, net.minecraft.client.model.ModelPart> parts = PlayerBodyParts.resolve(this.form, rig);
        mchorse.bbs_mod.utils.pose.Pose pose = this.form.disassemblyPose.get();
        pose.transforms.clear();

        for (java.util.Map.Entry<Part, net.minecraft.client.model.ModelPart> entry : parts.entrySet())
        {
            String bone = rig.name(entry.getValue());
            PoseTransform transform = pose.getOrCreate(bone);

            switch (entry.getKey())
            {
                case HEAD -> transform.translate.set(0F, -12F, 0F);
                case TORSO -> transform.translate.set(0F, 0F, 0F);
                case LEFT_ARM -> transform.translate.set(-8F, 0F, 0F);
                case RIGHT_ARM -> transform.translate.set(8F, 0F, 0F);
                case LEFT_LEG -> transform.translate.set(-4F, 12F, 0F);
                case RIGHT_LEG -> transform.translate.set(4F, 12F, 0F);
            }
        }

        this.form.bumpPoseVersion();
        this.disassemblyEditor.setPose(pose, "player_disassembly");
    }

    private void resetDisassembly()
    {
        if (this.form == null) return;

        this.form.disassemblyPose.get().transforms.clear();
        this.form.bumpPoseVersion();
        this.disassemblyEditor.setPose(this.form.disassemblyPose.get(), "player_disassembly");
    }

    @Override
    public void pickBone(String bone)
    {
        super.pickBone(bone);

        this.poseEditor.selectBone(bone);
    }
}
