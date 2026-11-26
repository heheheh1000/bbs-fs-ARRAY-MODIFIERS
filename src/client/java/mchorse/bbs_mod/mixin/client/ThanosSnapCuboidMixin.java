package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.renderers.mob.MobRenderContext;
import mchorse.bbs_mod.forms.renderers.mob.ThanosSnapFragments;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.Cuboid.class)
public abstract class ThanosSnapCuboidMixin
{
    @Inject(
        method = "renderCuboid(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbs$renderDissolvingCuboid(
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

        if (context == null || context.isPicking())
        {
            return;
        }

        float progress = context.dissolveProgress();

        if (progress <= 0F)
        {
            return;
        }

        if (ThanosSnapFragments.render(
            (ModelPart.Cuboid) (Object) this,
            entry,
            vertices,
            light,
            overlay,
            red,
            green,
            blue,
            alpha,
            progress
        ))
        {
            info.cancel();
        }
    }
}
