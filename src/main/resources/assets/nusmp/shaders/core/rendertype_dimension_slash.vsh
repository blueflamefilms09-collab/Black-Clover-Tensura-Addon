#version 150

// nusmp 0.56: Yami's Dimension Slash, a spatial fracture on a tall quad. The quad's U runs across the tear, V along it (0 top .. 1 bottom).
// Format POSITION_TEX_COLOR like the nusmp:fx shader: the layer pre-transforms the corners to view space.
in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
