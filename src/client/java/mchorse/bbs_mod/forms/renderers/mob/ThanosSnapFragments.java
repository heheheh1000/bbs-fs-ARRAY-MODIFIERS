package mchorse.bbs_mod.forms.renderers.mob;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Replaces each vanilla model cuboid with eight independently moving voxel shards while it
 * dissolves. Geometry is evaluated from the keyframed film progress, so scrubbing and export
 * render the same pose without a particle simulation or per-frame allocations in the world.
 */
public final class ThanosSnapFragments
{
    private ThanosSnapFragments()
    {}

    public static boolean render(
        ModelPart.Cuboid cuboid,
        MatrixStack.Entry entry,
        VertexConsumer vertices,
        int light,
        int overlay,
        float red,
        float green,
        float blue,
        float alpha,
        float progress
    )
    {
        int seed = mix(System.identityHashCode(cuboid));
        float threshold = ((seed >>> 1) & 0x7fffffff) / (float) Integer.MAX_VALUE * 0.72F;

        if (progress < threshold)
        {
            return false;
        }

        float age = (progress - threshold) / Math.max(0.0001F, 1F - threshold);

        if (age >= 0.96F)
        {
            return true;
        }

        float shrink = 1F - age * 0.72F;
        float brightness = Math.min(1F, age * 1.35F);
        float r = red + (1F - red) * brightness;
        float g = green + (0.78F - green) * brightness;
        float b = blue + (0.28F - blue) * brightness;

        float minX = cuboid.minX;
        float minY = cuboid.minY;
        float minZ = cuboid.minZ;
        float sizeX = (cuboid.maxX - minX) * 0.5F;
        float sizeY = (cuboid.maxY - minY) * 0.5F;
        float sizeZ = (cuboid.maxZ - minZ) * 0.5F;

        for (int i = 0; i < 8; i++)
        {
            int shardSeed = mix(seed + i * 0x9e3779b9);
            float rx = unit(shardSeed);
            float ry = unit(mix(shardSeed + 0x68bc21eb));
            float rz = unit(mix(shardSeed + 0x02e5be93));

            float x0 = minX + (i & 1) * sizeX;
            float y0 = minY + ((i >> 1) & 1) * sizeY;
            float z0 = minZ + ((i >> 2) & 1) * sizeZ;
            float cx = x0 + sizeX * 0.5F;
            float cy = y0 + sizeY * 0.5F;
            float cz = z0 + sizeZ * 0.5F;

            float dx = (rx - 0.5F) * age * 26F;
            float dy = (ry - 0.25F) * age * 24F + age * 5F;
            float dz = (rz - 0.5F) * age * 26F;

            float hx = sizeX * 0.5F * shrink;
            float hy = sizeY * 0.5F * shrink;
            float hz = sizeZ * 0.5F * shrink;

            drawCube(vertices, entry, cx + dx - hx, cy + dy - hy, cz + dz - hz,
                cx + dx + hx, cy + dy + hy, cz + dz + hz,
                r, g, b, alpha, light, overlay);
        }

        return true;
    }

    private static void drawCube(
        VertexConsumer out,
        MatrixStack.Entry entry,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float red, float green, float blue, float alpha,
        int light, int overlay
    )
    {
        face(out, entry, new float[][] {
            {x0,y0,z1}, {x1,y0,z1}, {x1,y1,z1}, {x0,y1,z1}
        }, 0F, 0F, 1F, red, green, blue, alpha, light, overlay);
        face(out, entry, new float[][] {
            {x1,y0,z0}, {x0,y0,z0}, {x0,y1,z0}, {x1,y1,z0}
        }, 0F, 0F, -1F, red, green, blue, alpha, light, overlay);
        face(out, entry, new float[][] {
            {x0,y0,z0}, {x0,y0,z1}, {x0,y1,z1}, {x0,y1,z0}
        }, -1F, 0F, 0F, red, green, blue, alpha, light, overlay);
        face(out, entry, new float[][] {
            {x1,y0,z1}, {x1,y0,z0}, {x1,y1,z0}, {x1,y1,z1}
        }, 1F, 0F, 0F, red, green, blue, alpha, light, overlay);
        face(out, entry, new float[][] {
            {x0,y0,z0}, {x1,y0,z0}, {x1,y0,z1}, {x0,y0,z1}
        }, 0F, -1F, 0F, red, green, blue, alpha, light, overlay);
        face(out, entry, new float[][] {
            {x0,y1,z1}, {x1,y1,z1}, {x1,y1,z0}, {x0,y1,z0}
        }, 0F, 1F, 0F, red, green, blue, alpha, light, overlay);
    }

    private static void face(
        VertexConsumer out,
        MatrixStack.Entry entry,
        float[][] points,
        float nx, float ny, float nz,
        float red, float green, float blue, float alpha,
        int light, int overlay
    )
    {
        for (float[] point : points)
        {
            out.vertex(entry.getPositionMatrix(), point[0], point[1], point[2])
                .color(red, green, blue, alpha)
                .texture(0.5F, 0.5F)
                .overlay(overlay)
                .light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz)
                .next();
        }
    }

    private static int mix(int value)
    {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        return value ^ value >>> 16;
    }

    private static float unit(int value)
    {
        return (value & 0x00ffffff) / 16777215F;
    }
}
