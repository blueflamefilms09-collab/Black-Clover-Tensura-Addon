#version 150

// nusmp 1.43.0: the event horizon. Sampler0 = textures/particle/portal_swirl.png (polar strip: u round the gate, v = distance from the
// middle). Sampler1 = a copy of the frame taken once before the gates were drawn. The horizon shows that copy bent by a swirl whose
// strength is PortalRefract (0 when the copy is not available: then only the swirl light is drawn), dark and violet towards the core, with
// the swirl texture's light on top. In warp mode (PortalMode > 0.5) the whole copy is redrawn with a faint radial ripple of strength PortalWarp.
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform vec2 ScreenSize;
uniform float PortalMode;
uniform float PortalRefract;
uniform float PortalWarp;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float t = GameTime * 24000.0;                       // ticks
    vec2 sc = gl_FragCoord.xy / ScreenSize;
    if (PortalMode > 0.5) {
        vec2 d = sc - 0.5;
        float len = length(d);
        float ripple = sin(len * 28.0 - t * 0.35) * 0.5 + 0.5;
        vec2 uv = sc + normalize(d + vec2(1e-5)) * ripple * 0.012 * PortalWarp * smoothstep(0.0, 0.5, len);
        float a = clamp(PortalWarp * 4.0, 0.0, 1.0);
        fragColor = vec4(texture(Sampler1, clamp(uv, 0.001, 0.999)).rgb, a);
        return;
    }
    float r = clamp(texCoord0.y, 0.0, 1.0);
    vec2 uv = texCoord0;
    uv.x += 0.03 * sin(t * 0.07 + uv.y * 9.0);
    vec4 swirl = texture(Sampler0, uv);
    // bend the copy: a ripple that turns with the swirl and weakens towards the rim, so the edge of the gate stays sharp
    float ph = texCoord0.x * 6.2831853 * 2.0 + t * 0.06;
    vec2 off = vec2(sin(ph + r * 7.0), cos(ph * 1.3 - r * 5.0)) * 0.035 * PortalRefract * (1.0 - r * 0.6);
    vec3 scene = texture(Sampler1, clamp(sc + off, 0.001, 0.999)).rgb;
    vec3 tint = vertexColor.rgb;
    float core = smoothstep(0.55, 0.0, r);
    vec3 seen = scene * mix(vec3(0.55), tint * 0.9, 0.35) * (1.0 - 0.75 * core);        // darker and more violet towards the core
    float edge = smoothstep(1.0, 0.82, r);
    // light of the swirl on top of the bent view
    vec3 light = swirl.rgb * swirl.a * tint * 1.6;
    vec3 col = mix(vec3(0.04, 0.02, 0.08) * tint, seen, clamp(PortalRefract, 0.0, 1.0)) + light;
    float alpha = vertexColor.a * edge * mix(0.75, 0.95, clamp(PortalRefract, 0.0, 1.0));
    vec4 o = vec4(col, alpha) * ColorModulator;
    if (o.a < 0.01) discard;
    fragColor = o;
}
