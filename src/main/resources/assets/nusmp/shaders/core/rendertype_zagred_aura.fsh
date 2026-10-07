#version 150

// nusmp 0.48: Zagred's aura - the rim pulses from violet (#8000FF) to crimson (#FF0055), drawn additively over the body.
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in float rim;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) {
        discard;
    }
    float t = GameTime * 1200.0;                                   // ~ seconds
    float pulse = 0.5 + 0.5 * sin(t * 2.2 + texCoord0.y * 24.0);
    vec3 col = mix(vec3(0.502, 0.0, 1.0), vec3(1.0, 0.0, 0.333), pulse);
    float f = pow(clamp(rim, 0.0, 1.0), 2.4) * (0.6 + 0.4 * pulse);
    fragColor = vec4(col * f * vertexColor.rgb * vertexColor.a, 1.0) * ColorModulator;
}
