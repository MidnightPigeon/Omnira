package com.mcmagic.omnira.client;

/** Idle world motion and stable material-stack offsets. */
public final class MiniatureWorldMotion {
    public record Copy(float x, float y, float z) {}
    private static final Copy[] STACK_OFFSETS = {
        new Copy(0, 0, 0), new Copy(-.18F, .12F, .12F), new Copy(.18F, .24F, -.12F),
        new Copy(-.13F, .36F, -.16F), new Copy(.13F, .48F, .16F)
    };

    public static int stackCopies(int count) {
        return count <= 0 ? 0 : count == 1 ? 1 : Math.min(5, 2 + (count - 1) / 16);
    }

    public static Copy stackOffset(int copy) { return STACK_OFFSETS[copy]; }

    public static float smooth(float value) {
        float p = Math.max(0, Math.min(1, value));
        return p * p * (3 - 2 * p);
    }

    public static float rotation(double time) { return (float) ((time * .075) % 360); }
    public static float bob(double time) { return .12F + (float) Math.sin(time * .015) * .045F; }

    private MiniatureWorldMotion() {}
}
