package com.mcmagic.omnira.codex.edit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Integer lattice geometry; traversal order is bottom-up, then Z, then X. */
public final class EditGeometry {
    private EditGeometry() {}

    public static int diameter(long allocatedMPU) {
        return allocatedMPU <= 8192 ? 7 : 7 + 2 * (int)Math.min(4, (allocatedMPU - 8192L)/2048L);
    }

    public static boolean validDiameter(int diameter) {
        return diameter >= 7 && diameter <= 15 && (diameter & 1) == 1;
    }

    public static int cubeHalfExtent(int radius) {
        int half = 0;
        while (3 * (half + 1) * (half + 1) <= radius * radius) half++;
        return half;
    }

    /** Keep only cells touching exterior air along a face; the remaining interior stays enclosed. */
    public static boolean ecologyShell(int radius,int x,int y,int z){
        int distance=x*x+y*y+z*z;
        return distance<=radius*radius&&distance+2*Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))+1>radius*radius;
    }

    public static boolean contains(Operation op, int radius, Direction.Axis axis, boolean hollow,
                                   int x, int y, int z) {
        int distance = x*x + y*y + z*z;
        if (op == Operation.INSCRIBED) {
            int half = cubeHalfExtent(radius);
            int edge = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
            return edge <= half && (!hollow || edge == half);
        }
        if (op == Operation.PLANE) {
            int normal = axis.choose(x, y, z);
            int planar = distance - normal*normal;
            return normal == 0 && planar <= radius*radius &&
                    (!hollow || planar > (radius-2)*(radius-2));
        }
        return distance <= radius*radius &&
                (!(hollow && op == Operation.SHAPE) || distance > (radius-1)*(radius-1));
    }

    public static BlockPos offset(int cursor, int diameter) {
        int radius = (diameter-1)/2;
        return new BlockPos(cursor % diameter-radius, cursor/(diameter*diameter)-radius,
                cursor/diameter % diameter-radius);
    }
}
