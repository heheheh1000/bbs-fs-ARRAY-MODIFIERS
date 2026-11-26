package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.api.client.events.FormRenderEvents;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

/**
 * Keeps the Thanos form controls deterministic and hides the form only after its cuboids have
 * completely disintegrated. The actual body replacement is performed in ModelPartMixin.
 */
public final class ThanosSnapRenderer
{
    private ThanosSnapRenderer()
    {}

    public static void register()
    {
        FormRenderEvents.BEFORE.register(ThanosSnapRenderer::before);
        FormRenderEvents.AFTER.register(ThanosSnapRenderer::after);
    }

    private static void before(Form form, FormRenderingContext context)
    {
        if (context == null || context.ui || !form.thanosSnapEnabled.get())
        {
            return;
        }

        float progress = clamp(form.thanosSnapProgress.get(), 0F, 1F);

        if (progress >= 0.999F && context.stack != null)
        {
            context.stack.push();
            context.stack.scale(0F, 0F, 0F);
            context.stack.push();
            HIDDEN_FOR_PASS.add(form);
        }
    }

    private static final java.util.Set<Form> HIDDEN_FOR_PASS = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private static void after(Form form, FormRenderingContext context)
    {
        if (HIDDEN_FOR_PASS.remove(form) && context != null && context.stack != null)
        {
            context.stack.pop();
            context.stack.pop();
        }
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
