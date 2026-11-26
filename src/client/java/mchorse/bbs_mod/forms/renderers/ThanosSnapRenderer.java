package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.api.client.events.FormRenderEvents;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * First-pass Thanos Snap effect for BBS forms.
 *
 * <p>The progress property is an ordinary BBS value, so the film timeline can keyframe it.
 * White-ash particles are emitted deterministically once per world tick. At the end of the
 * animation the form is hidden for the render pass while already-emitted particles finish.</p>
 *
 * <p>This intentionally uses vanilla particles and the public render funnel; it does not replace
 * BBS's model renderer or mutate the form's visibility value.</p>
 */
public final class ThanosSnapRenderer
{
    private static final Map<Form, Long> LAST_EMISSION = new WeakHashMap<>();
    private static final Set<Form> HIDDEN_FOR_PASS = Collections.newSetFromMap(new IdentityHashMap<>());

    private ThanosSnapRenderer()
    {}

    public static void register()
    {
        FormRenderEvents.BEFORE.register(ThanosSnapRenderer::before);
        FormRenderEvents.AFTER.register(ThanosSnapRenderer::after);
    }

    private static void before(Form form, FormRenderingContext context)
    {
        if (context == null || context.ui)
        {
            return;
        }

        if (!form.thanosSnapEnabled.get())
        {
            return;
        }

        float progress = clamp(form.thanosSnapProgress.get(), 0F, 1F);

        if (progress <= 0F)
        {
            return;
        }

        emitParticles(form, context, progress);

        /* Keep the stack balanced through AFTER even if the form renderer throws. */
        if (progress >= 0.999F)
        {
            context.stack.push();
            context.stack.scale(0F, 0F, 0F);
            HIDDEN_FOR_PASS.add(form);
        }
    }

    private static void after(Form form, FormRenderingContext context)
    {
        if (HIDDEN_FOR_PASS.remove(form) && context != null && context.stack != null)
        {
            context.stack.pop();
        }
    }

    private static void emitParticles(Form form, FormRenderingContext context, float progress)
    {
        if (context.entity == null)
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;

        if (world == null || !world.isClient)
        {
            return;
        }

        long tick = world.getTime();
        Long previousTick = LAST_EMISSION.get(form);

        /* BBS may render a form more than once per frame (outlines, passes, etc.). */
        if (previousTick != null && previousTick.longValue() == tick)
        {
            return;
        }

        LAST_EMISSION.put(form, tick);

        float density = clamp(form.thanosSnapDensity.get(), 1F, 64F);
        float spread = clamp(form.thanosSnapSpread.get(), 0.1F, 3F);
        float emission = Math.min(1F, progress * 2F) * Math.min(1F, (1F - progress) * 5F);
        int count = Math.min(64, Math.round(density * emission));

        if (count <= 0)
        {
            return;
        }

        double centerX = context.entity.getX();
        double centerY = context.entity.getY();
        double centerZ = context.entity.getZ();
        Random random = new Random(((long) System.identityHashCode(form) << 32) ^ tick);

        double windX = form.thanosSnapWindX.get();
        double windY = form.thanosSnapWindY.get();
        double windZ = form.thanosSnapWindZ.get();

        for (int i = 0; i < count; i++)
        {
            double x = centerX + (random.nextDouble() - 0.5D) * 0.7D * spread;
            double y = centerY + random.nextDouble() * 1.8D * spread;
            double z = centerZ + (random.nextDouble() - 0.5D) * 0.7D * spread;

            double vx = windX + (random.nextDouble() - 0.5D) * 0.12D * spread;
            double vy = windY + random.nextDouble() * 0.12D;
            double vz = windZ + (random.nextDouble() - 0.5D) * 0.12D * spread;

            world.addParticle(ParticleTypes.WHITE_ASH, x, y, z, vx, vy, vz);
        }
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
