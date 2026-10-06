#version 150

// nusmp FX: fine detail lives here so it costs no vertices.
// FxShimmer: heat / water refraction wobble of the texture. FxPulse: slow brightness pulse (max 25%).
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform float FxShimmer;
uniform float FxPulse;
uniform float FxSeed;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float t = GameTime * 1200.0 + FxSeed;   // GameTime is a day fraction: x1200 ~ seconds
    vec2 uv = texCoord0 + FxShimmer * 0.035 * vec2(sin(texCoord0.y * 23.0 + t * 6.0), cos(texCoord0.x * 19.0 + t * 5.0));
    vec4 c = texture(Sampler0, uv) * vertexColor;
    c.rgb *= 1.0 + FxPulse * 0.25 * sin(t * 7.0);
    c *= ColorModulator;
    if (c.a < 0.004) discard;
    fragColor = c;
}
