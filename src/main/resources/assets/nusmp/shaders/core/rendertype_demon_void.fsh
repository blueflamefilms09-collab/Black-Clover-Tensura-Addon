#version 150

// nusmp 0.48: the end portal's parallax star layers, recoloured crimson and black, drifting faster, with a slow pulse
// rising from the depths. Sampler0 = the end sky, Sampler1 = the end portal's star layer (vanilla textures).
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float GameTime;

in vec4 texProj0;

out vec4 fragColor;

const vec3[] COLORS = vec3[](
    vec3(0.55, 0.05, 0.10), vec3(0.42, 0.02, 0.08), vec3(0.30, 0.03, 0.12), vec3(0.62, 0.08, 0.10), vec3(0.20, 0.02, 0.06),
    vec3(0.48, 0.04, 0.14), vec3(0.36, 0.06, 0.06), vec3(0.70, 0.12, 0.16), vec3(0.25, 0.01, 0.10), vec3(0.58, 0.03, 0.05)
);

mat2 rotation(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, -s, s, c);
}

void main() {
    vec2 screen = texProj0.xy / texProj0.w;
    float t = GameTime * 1200.0;                                  // GameTime is a fraction of a day: x1200 ~ seconds
    vec3 color = texture(Sampler0, screen * 1.5 + vec2(0.0, t * 0.004)).rgb * vec3(0.10, 0.02, 0.03);
    for (int i = 0; i < 10; i++) {
        float layer = float(i + 1);
        vec2 p = rotation(radians((layer * layer * 4321.0 + layer * 9.0) * 2.0)) * (screen * (4.5 - layer / 4.0));
        p += vec2(17.0 / layer, (2.0 + layer / 1.5) * t * 0.02);
        color += texture(Sampler1, p).rgb * COLORS[i];
    }
    float pulse = 0.5 + 0.5 * sin(t * 2.4);
    color += vec3(0.30, 0.0, 0.04) * pulse * 0.35;
    fragColor = vec4(color, 1.0);
}
