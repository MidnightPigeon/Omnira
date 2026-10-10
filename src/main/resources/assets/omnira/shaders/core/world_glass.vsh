#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
flat out int materialFlags;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    materialFlags = UV1.x / 32;
    bool unlit = (materialFlags & 2) != 0;
    vertexDistance = fog_distance(Position, FogShape);
    vertexColor = unlit ? Color : minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    lightMapColor = unlit ? vec4(1.0) : texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = unlit ? vec4(0.0, 0.0, 0.0, 1.0) : texelFetch(Sampler1, ivec2(UV1.x % 32, UV1.y), 0);
    texCoord0 = UV0;
}
