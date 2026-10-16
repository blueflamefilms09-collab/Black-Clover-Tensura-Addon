#version 150

// nusmp 1.43.0: the spatial portal's event horizon and the full-screen warp pulse. Format POSITION_TEX_COLOR; for the horizon the
// layer pre-transforms the corners to view space (like the nusmp:fx shader); in warp mode (PortalMode > 0.5) Position.xy is already clip space.
in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float PortalMode;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    if (PortalMode > 0.5) gl_Position = vec4(Position.xy, 0.0, 1.0);
    else gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
