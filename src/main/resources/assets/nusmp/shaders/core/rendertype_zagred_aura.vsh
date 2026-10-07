#version 150

// nusmp 0.48: Zagred's aura - a fresnel rim computed per vertex in view space (strongest where the surface turns away).
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;
out float rim;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    vec3 n = normalize(mat3(ModelViewMat) * Normal);
    vec3 toEye = normalize(-viewPos.xyz);
    rim = 1.0 - abs(dot(n, toEye));
    vertexColor = Color;
    texCoord0 = UV0;
}
