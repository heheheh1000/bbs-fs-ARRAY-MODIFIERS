package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.renderers.mob.IBBSModelPart;
import mchorse.bbs_mod.forms.renderers.mob.MobRenderContext;
import mchorse.bbs_mod.forms.renderers.mob.ThanosSnapFragments;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements IBBSModelPart
{
    @Shadow @Final private Map<String, ModelPart> children;
    @Shadow @Final private List<ModelPart.Cuboid> cuboids;

    @Unique
    private String bbs$name;

    @Unique
    private ModelPart bbs$parent;

    @Override
    public String bbs$getName()
    {
        return this.bbs$name;
    }

    @Override
    public void bbs$setName(String name)
    {
        this.bbs$name = name;
    }

    @Override
    public ModelPart bbs$getParent()
    {
        return this.bbs$parent;
    }

    @Override
    public void bbs$setParent(ModelPart parent)
    {
        this.bbs$parent = parent;
    }

    @Override
    public Map<String, ModelPart> bbs$children()
    {
        return this.children;
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int bbs$pickingLight(int light)
    {
        MobRenderContext context = MobRenderContext.current();

        return context == null || !context.isPicking() ? light : context.partLight((ModelPart) (Object) this);
    }

    /**
     * Replace the model part's complete cuboid pass at the point where vanilla has already
     * applied this bone's transform. This is deliberately on ModelPart.renderCuboids rather
     * than Cuboid.renderCuboid: it is the common vanilla entry point for every cuboid and avoids
     * depending on the nested cuboid method being reached by optimized render paths.
     */
    @Inject(
        method = "renderCuboids(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbs$renderSnapCuboids(
        MatrixStack.Entry entry,
        VertexConsumer vertices,
        int light,
        int overlay,
        float red,
        float green,
        float blue,
        float alpha,
        CallbackInfo info
    )
    {
        MobRenderContext context = MobRenderContext.current();

        if (context == null || context.isPicking() || context.dissolveProgress() <= 0F)
        {
            return;
        }

        float progress = context.dissolveProgress();

        for (ModelPart.Cuboid cuboid : this.cuboids)
        {
            if (!ThanosSnapFragments.render(cuboid, entry, vertices, light, overlay, red, green, blue, alpha, progress))
            {
                cuboid.renderCuboid(entry, vertices, light, overlay, red, green, blue, alpha);
            }
        }

        /* We rendered every cuboid ourselves: never let vanilla draw the intact body over it. */
        info.cancel();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbs$nameChildren(List<ModelPart.Cuboid> cuboids, Map<String, ModelPart> children, CallbackInfo info)
    {
        for (Map.Entry<String, ModelPart> entry : children.entrySet())
        {
            IBBSModelPart child = (IBBSModelPart) (Object) entry.getValue();

            child.bbs$setName(entry.getKey());
            child.bbs$setParent((ModelPart) (Object) this);
        }
    }
}
