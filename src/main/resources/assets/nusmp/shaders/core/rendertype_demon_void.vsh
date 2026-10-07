#version 150

// nusmp 0.48: the Genesis Demon-Slayer's void (the dark spots and the central split).
// Screen-space projection like the end portal's: the void stays still on screen while the blade moves across it.
in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 texProj0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vec4 h = gl_Position * 0.5;
    texProj0 = vec4(h.x + h.w, h.y + h.w, gl_Position.z, gl_Position.w);
}
