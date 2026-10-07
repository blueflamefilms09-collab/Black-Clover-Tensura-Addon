#version 150

// nusmp 0.56: the Dimension Slash. A pitch-black core line with a white-violet rim, round it chaotic jagged purple / magenta energy
// that shifts over time (the UV x is jittered by a hash of the row and the time step), tiny stars twinkling inside the void.
// Sampler0 = textures/particle/dark_slash_tear.png: its alpha is the jagged silhouette of the aura. The vertex colour tints the
// aura a little (alpha = overall fade). The jitter is stepped (about 14 times a second) so the edge crawls instead of blurring.
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }

void main() {
    float t = GameTime * 1200.0;                                  // GameTime is a fraction of a day: x1200 ~ seconds
    vec2 uv = texCoord0;
    float taper = pow(clamp(sin(3.14159265 * uv.y), 0.0, 1.0), 0.6);          // the tear is pointed at both ends
    float tick = floor(t * 14.0);
    float row = floor(uv.y * 44.0);
    float jag = (hash(vec2(row, tick)) - 0.5) * 0.09 + (hash(vec2(row * 1.7 + 3.0, tick + 11.0)) - 0.5) * 0.04;
    float ripple = sin(uv.y * 31.0 - t * 8.0) * 0.012 + sin(uv.y * 13.0 + t * 3.0) * 0.018;
    uv.x += (jag + ripple) * taper;                               // the owner's jitter, kept: x displaced by a hash of y and time

    float d = abs(uv.x - 0.5);
    vec4 baseTex = texture(Sampler0, uv);                         // alpha: the jagged aura silhouette (baked)
    float coreW = 0.058 * taper;
    float core = smoothstep(coreW + 0.006, coreW - 0.006, d);
    float rim = exp(-pow((d - coreW) / 0.0075, 2.0)) * taper;
    float edge = clamp((d - coreW) / max(0.5 - coreW, 0.01), 0.0, 1.0);
    float pulse = 0.5 + 0.5 * sin(t * 6.0 + uv.y * 17.0);

    // the aura: deep violet near the core, magenta where it flickers, fading to the edge
    float flick = hash(vec2(floor(uv.y * 90.0), tick * 0.5));
    vec3 violet = vec3(0.40, 0.04, 0.78);
    vec3 magenta = vec3(0.95, 0.14, 0.72);
    vec3 auraCol = mix(violet, magenta, clamp(edge * 1.2 + (flick - 0.5) * 0.6 + pulse * 0.12, 0.0, 1.0));
    auraCol = mix(auraCol, vec3(0.85, 0.7, 1.0), pow(1.0 - edge, 6.0) * 0.55);       // white-hot right at the rim
    auraCol *= mix(vec3(1.0), vertexColor.rgb * 2.0, 0.25);
    float aura = pow(1.0 - edge, 1.7) * baseTex.a;
    aura = max(aura, baseTex.a * 0.35 * (1.0 - edge));

    // stars twinkling inside the void
    vec2 cell = floor(uv * vec2(14.0, 56.0));
    float h = hash(cell);
    vec2 inCell = fract(uv * vec2(14.0, 56.0)) - 0.5;
    float star = step(0.9, h) * smoothstep(0.22, 0.0, length(inCell)) * (0.5 + 0.5 * sin(t * 5.0 + h * 60.0));
    star *= step(d, coreW * 0.85);

    vec3 col = auraCol;
    float alpha = aura * 0.92;
    col = mix(col, vec3(0.012, 0.006, 0.022), core);                         // the pitch-black core
    alpha = max(alpha, core);
    col = mix(col, vec3(0.93, 0.86, 1.0), clamp(rim * (0.8 + 0.2 * pulse), 0.0, 1.0));      // white-violet hairline round the void
    alpha = max(alpha, rim * 0.95);
    col += vec3(0.9, 0.8, 1.0) * star * core;
    alpha *= vertexColor.a;
    vec4 outC = vec4(col, alpha) * ColorModulator;
    if (outC.a < 0.01) discard;
    fragColor = outC;
}
