package com.mcmagic.omnira.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Shared sky geometry for the renderer and headless visual preview; no game or GL state. */
public final class SpacetimeSkyMesh {
    @FunctionalInterface
    public interface VertexSink { void vertex(float x, float y, float z, int color); }
    private record Vertex(float x, float y, float z, int color) {}
    private record Pixel(int x, int z, double phase) {}
    private static final List<Vertex> BACKGROUND = background();
    private static final List<Vertex> STARS = stars();
    private static final List<Pixel> INFINITY = infinity();
    private static final double TAU = Math.PI * 2;

    private SpacetimeSkyMesh() {}

    public static void render(double seconds, float forest, VertexSink sink) {
        render(seconds,forest,0,sink);
    }

    public static void render(double seconds, float forest, float ruins, VertexSink sink) {
        VertexSink tinted = (x, y, z, color) -> sink.vertex(x, y, z, ruinsTint(tint(color, forest),ruins));
        for (Vertex v : BACKGROUND) tinted.vertex(v.x, v.y, v.z, v.color);
        for (Vertex v : STARS) tinted.vertex(v.x, v.y, v.z, v.color);
        aurora(seconds, tinted);
        for (Pixel pixel : INFINITY) {
            double wave = .5 + .5 * Math.cos(pixel.phase - seconds * .8);
            int color = argb(255, (int)(150 + 100 * wave), (int)(213 + 40 * wave), 255);
            // Only the color flows. The glyph stays centered at the zenith, without daily rotation.
            flatPixel(tinted, pixel.x, 80, pixel.z, 1, color);
        }
    }

    public static int tint(int color, float forest) {
        float amount = Math.max(0, Math.min(1, forest)) * .15F;
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        return argb(color >>> 24, (int)(r + (173 - r) * amount),
                (int)(g + (229 - g) * amount), (int)(b + (184 - b) * amount));
    }

    public static int ruinsTint(int color,float weight) {
        float amount=Math.clamp(weight,0,1)*.26F;
        int r=color>>16&255,g=color>>8&255,b=color&255;
        return argb(color>>>24,(int)(r+(154-r)*amount),(int)(g+(115-g)*amount),(int)(b+(78-b)*amount));
    }

    private static List<Vertex> background() {
        List<Vertex> vertices = new ArrayList<>();
        for (int lat = 0; lat < 32; lat++) for (int lon = 0; lon < 96; lon++) {
            for (int corner = 0; corner < 4; corner++) {
                double elevation = -Math.PI / 2 + Math.PI * (lat + (corner >= 2 ? 1 : 0)) / 32;
                double azimuth = 2 * Math.PI * (lon + (corner == 1 || corner == 2 ? 1 : 0)) / 96;
                double horizon = Math.pow(1 - Math.abs(Math.sin(elevation)), 2);
                int color = argb(255, (int)(35 + 26 * horizon), (int)(57 + 34 * horizon), (int)(88 + 30 * horizon));
                spherical((x,y,z,c) -> vertices.add(new Vertex(x,y,z,c)), azimuth, elevation, 110, color);
            }
        }
        return List.copyOf(vertices);
    }

    private static List<Vertex> stars() {
        List<Vertex> vertices = new ArrayList<>();
        Random random = new Random(0x5350414345L);
        for (int i = 0; i < 450; i++) {
            double azimuth = random.nextDouble() * Math.PI * 2;
            double elevation = Math.asin(random.nextDouble());
            double size = .0008 + random.nextDouble() * .0012;
            int color = i % 4 == 0 ? 0xFFC6F5ED : 0xFFDCE6FF;
            for (int corner = 0; corner < 4; corner++) {
                spherical((x,y,z,c) -> vertices.add(new Vertex(x,y,z,c)),
                        azimuth + (corner == 1 || corner == 2 ? size : -size) / Math.max(.08, Math.cos(elevation)),
                        elevation + (corner >= 2 ? size : -size), 100, color);
            }
        }
        return List.copyOf(vertices);
    }

    private static void aurora(double time, VertexSink sink) {
        for (int ribbon = 0; ribbon < 3; ribbon++) for (int col = 0; col < 144; col++) {
            double angle = TAU * col / 144;
            double curtain = .5 + .5 * Math.sin(angle * 17 + ribbon * 2 + time * .35);
            double envelope = .45 + .55 * Math.pow(.5 + .5 * Math.sin(angle * 2 - ribbon), 2);
            for (int row = 0; row < 12; row++) {
                double fraction = row / 12.0;
                double fade = Math.pow(1 - fraction, 1.7) * Math.min(1, (row + 1) / 2.0);
                int alpha = (int)(115 * envelope * fade * (.55 + .45 * curtain));
                int color = argb(alpha, 60 + row * 3, 205 - row * 5, 173 + row * 6);
                for (int corner = 0; corner < 4; corner++) {
                    double a = TAU * (col + (corner == 1 || corner == 2 ? 1 : 0)) / 144;
                    double base = .18 + ribbon * .23 + .065 * Math.sin(a * 3 + ribbon + time * .07)
                            + .035 * Math.sin(a * 7 - time * .11);
                    double height = .19 + .07 * Math.sin(a * 5 + ribbon + time * .13);
                    spherical(sink, a, base + height * (row + (corner >= 2 ? 1 : 0)) / 12, 95, color);
                }
            }
        }
    }

    private static List<Pixel> infinity() {
        List<Pixel> pixels = new ArrayList<>();
        for (int z = -8; z < 8; z++) for (int x = -17; x < 17; x++) {
            double nearest = Double.MAX_VALUE, phase = 0;
            for (int step = 0; step < 720; step++) {
                double t = 2 * Math.PI * step / 720;
                double dx = x + .5 - 14 * Math.cos(t), dz = z + .5 - 6 * Math.sin(2 * t);
                double distance = dx * dx + dz * dz;
                if (distance < nearest) { nearest = distance; phase = t; }
            }
            if (nearest < 1.15) pixels.add(new Pixel(x, z, phase));
        }
        return List.copyOf(pixels);
    }

    private static void flatPixel(VertexSink sink, float x, float y, float z, float size, int color) {
        sink.vertex(x,y,z,color); sink.vertex(x+size,y,z,color);
        sink.vertex(x+size,y,z+size,color); sink.vertex(x,y,z+size,color);
    }

    private static void spherical(VertexSink sink, double a, double e, double radius, int color) {
        sink.vertex((float)(Math.cos(e)*Math.cos(a)*radius), (float)(Math.sin(e)*radius),
                (float)(Math.cos(e)*Math.sin(a)*radius), color);
    }

    private static int argb(int a, int r, int g, int b) {
        return a << 24 | r << 16 | g << 8 | b;
    }
}
