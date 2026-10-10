#version 150

#moj_import <fog.glsl>
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
flat in int materialFlags;
out vec4 fragColor;

void main() {
    // Preserve the original per-material culling and unlit-color behavior in one quad batch.
    if ((materialFlags & 1) != 0 && !gl_FrontFacing) discard;
    bool unlit = (materialFlags & 2) != 0;
    vec4 texel = texture(Sampler0, texCoord0);
    if (!unlit && texel.a < 0.1) discard;
    vec4 color = texel * vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    fragColor = unlit ? color : linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
