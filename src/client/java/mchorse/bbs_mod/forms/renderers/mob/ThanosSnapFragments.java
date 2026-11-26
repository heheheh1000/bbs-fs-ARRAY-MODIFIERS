package mchorse.bbs_mod.forms.renderers.mob;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

/**
 * Deterministic, cuboid-local Thanos disintegration. Each vanilla cuboid is divided into a
 * 3x3x3 grid and every shard samples the original cuboid face UVs. All motion is a pure
 * function of film progress, so timeline scrubbing and export are repeatable.
 */
public final class ThanosSnapFragments
{
    private static final int DIVISIONS = 3;

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
        float minX = cuboid.minX;
        float minY = cuboid.minY;
        float minZ = cuboid.minZ;
        float maxX = cuboid.maxX;
        float maxY = cuboid.maxY;
        float maxZ = cuboid.maxZ;

        int seed = mix(Float.floatToIntBits(minX) ^ Integer.rotateLeft(Float.floatToIntBits(minY), 7)
            ^ Integer.rotateLeft(Float.floatToIntBits(minZ), 13)
            ^ Integer.rotateLeft(Float.floatToIntBits(maxX), 17)
            ^ Integer.rotateLeft(Float.floatToIntBits(maxY), 23)
            ^ Integer.rotateLeft(Float.floatToIntBits(maxZ), 29));

        float cuboidThreshold = unit(seed) * 0.18F;

        if (progress < cuboidThreshold)
        {
            return false;
        }

        float age = (progress - cuboidThreshold) / Math.max(0.0001F, 1F - cuboidThreshold);

        if (age >= 0.995F)
        {
            return true;
        }

        float sizeX = (maxX - minX) / DIVISIONS;
        float sizeY = (maxY - minY) / DIVISIONS;
        float sizeZ = (maxZ - minZ) / DIVISIONS;

        for (int ix = 0; ix < DIVISIONS; ix++)
        {
            for (int iy = 0; iy < DIVISIONS; iy++)
            {
                for (int iz = 0; iz < DIVISIONS; iz++)
                {
                    int index = ix + iy * DIVISIONS + iz * DIVISIONS * DIVISIONS;
                    int shardSeed = mix(seed ^ (index * 0x9e3779b9));
                    float delay = unit(shardSeed) * 0.22F;
                    float shardAge = clamp((age - delay) / (1F - delay), 0F, 1F);

                    if (shardAge >= 0.97F)
                    {
                        continue;
                    }

                    float x0 = minX + ix * sizeX;
                    float y0 = minY + iy * sizeY;
                    float z0 = minZ + iz * sizeZ;
                    float cx = x0 + sizeX * 0.5F;
                    float cy = y0 + sizeY * 0.5F;
                    float cz = z0 + sizeZ * 0.5F;

                    float rx = unit(mix(shardSeed + 0x68bc21eb));
                    float ry = unit(mix(shardSeed + 0x02e5be93));
                    float rz = unit(mix(shardSeed + 0x967a889b));

                    float distance = shardAge * shardAge;
                    float dx = (rx - 0.5F) * distance * 22F;
                    float dy = (ry - 0.35F) * distance * 24F + distance * 3.5F;
                    float dz = (rz - 0.5F) * distance * 22F;
                    float shrink = 1F - shardAge * 0.78F;

                    float hx = sizeX * 0.5F * shrink;
                    float hy = sizeY * 0.5F * shrink;
                    float hz = sizeZ * 0.5F * shrink;

                    float sx0 = cx + dx - hx;
                    float sy0 = cy + dy - hy;
                    float sz0 = cz + dz - hz;
                    float sx1 = cx + dx + hx;
                    float sy1 = cy + dy + hy;
                    float sz1 = cz + dz + hz;

                    float glow = clamp(shardAge * 1.8F, 0F, 1F);
                    float r = red + (1F - red) * glow;
                    float g = green + (0.78F - green) * glow;
                    float b = blue + (0.30F - blue) * glow;

                    drawCube(cuboid, vertices, entry, sx0, sy0, sz0, sx1, sy1, sz1,
                        r, g, b, alpha, light, overlay);
                }
            }
        }

        return true;
    }

    private static void drawCube(
        ModelPart.Cuboid source,
        VertexConsumer out,
        MatrixStack.Entry entry,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float red, float green, float blue, float alpha,
        int light, int overlay
    )
    {
        face(source, out, entry, new float[][] {{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}},
            0F, 0F, 1F, red, green, blue, alpha, light, overlay);
        face(source, out, entry, new float[][] {{x1,y0,z0},{x0,y0,z0},{x0,y1,z0},{x1,y1,z0}},
            0F, 0F, -1F, red, green, blue, alpha, light, overlay);
        face(source, out, entry, new float[][] {{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z0}},
            -1F, 0F, 0F, red, green, blue, alpha, light, overlay);
        face(source, out, entry, new float[][] {{x1,y0,z1},{x1,y0,z0},{x1,y1,z0},{x1,y1,z1}},
            1F, 0F, 0F, red, green, blue, alpha, light, overlay);
        face(source, out, entry, new float[][] {{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1}},
            0F, -1F, 0F, red, green, blue, alpha, light, overlay);
        face(source, out, entry, new float[][] {{x0,y1,z1},{x1,y1,z1},{x1,y1,z0},{x0,y1,z0}},
            0F, 1F, 0F, red, green, blue, alpha, light, overlay);
    }

    private static void face(
        ModelPart.Cuboid source,
        VertexConsumer out,
        MatrixStack.Entry entry,
        float[][] points,
        float nx, float ny, float nz,
        float red, float green, float blue, float alpha,
        int light, int overlay
    )
    {
        ModelPart.Quad quad = findFace(source, nx, ny, nz);

        for (float[] point : points)
        {
            float[] uv = sampleUv(quad, nx, ny, nz, point[0], point[1], point[2]);

            out.vertex(entry.getPositionMatrix(), point[0], point[1], point[2])
                .color(red, green, blue, alpha)
                .texture(uv[0], uv[1])
                .overlay(overlay)
                .light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz)
                .next();
        }
    }

    private static ModelPart.Quad findFace(ModelPart.Cuboid cuboid, float nx, float ny, float nz)
    {
        ModelPart.Quad best = null;
        float bestDot = -Float.MAX_VALUE;

        for (ModelPart.Quad quad : cuboid.sides)
        {
            float dot = quad.direction.x * nx + quad.direction.y * ny + quad.direction.z * nz;

            if (dot > bestDot)
            {
                bestDot = dot;
                best = quad;
            }
        }

        return best;
    }

    private static float[] sampleUv(ModelPart.Quad quad, float nx, float ny, float nz, float x, float y, float z)
    {
        int axisA = nx != 0F ? 2 : 0;
        int axisB = ny != 0F ? 2 : 1;

        if (nz != 0F)
        {
            axisA = 0;
            axisB = 1;
        }
        else if (ny != 0F)
        {
            axisA = 0;
            axisB = 2;
        }

        float minA = Float.MAX_VALUE;
        float maxA = -Float.MAX_VALUE;
        float minB = Float.MAX_VALUE;
        float maxB = -Float.MAX_VALUE;

        for (ModelPart.Vertex vertex : quad.vertices)
        {
            float a = component(vertex.pos, axisA);
            float b = component(vertex.pos, axisB);
            minA = Math.min(minA, a);
            maxA = Math.max(maxA, a);
            minB = Math.min(minB, b);
            maxB = Math.max(maxB, b);
        }

        float[] uv00 = cornerUv(quad, axisA, axisB, minA, minB);
        float[] uv10 = cornerUv(quad, axisA, axisB, maxA, minB);
        float[] uv01 = cornerUv(quad, axisA, axisB, minA, maxB);

        float a = axisA == 0 ? x : axisA == 1 ? y : z;
        float b = axisB == 0 ? x : axisB == 1 ? y : z;
        float ta = maxA == minA ? 0F : (a - minA) / (maxA - minA);
        float tb = maxB == minB ? 0F : (b - minB) / (maxB - minB);

        return new float[] {
            uv00[0] + ta * (uv10[0] - uv00[0]) + tb * (uv01[0] - uv00[0]),
            uv00[1] + ta * (uv10[1] - uv00[1]) + tb * (uv01[1] - uv00[1])
        };
    }

    private static float[] cornerUv(ModelPart.Quad quad, int axisA, int axisB, float a, float b)
    {
        ModelPart.Vertex best = quad.vertices[0];
        float bestDistance = Float.MAX_VALUE;

        for (ModelPart.Vertex vertex : quad.vertices)
        {
            float da = component(vertex.pos, axisA) - a;
            float db = component(vertex.pos, axisB) - b;
            float distance = da * da + db * db;

            if (distance < bestDistance)
            {
                bestDistance = distance;
                best = vertex;
            }
        }

        return new float[] {best.u, best.v};
    }

    private static float component(Vector3f vector, int axis)
    {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
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
