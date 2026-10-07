# Dimension Slash shader (owner spec, adapted)

The owner supplied this as Forge code in a `crossover` namespace. In this mod (NeoForge 1.21.1, namespace `nusmp`) the equivalents are:
`assets/nusmp/shaders/core/rendertype_dimension_slash.{json,vsh,fsh}`, registered in `client/NUShaders.java` (see how `demon_void` and `zagred_aura` are loaded)
and a `RenderType` helper next to the other render types (`client/NURenderTypes.java`). Item tags use the 1.21 folder `data/nusmp/tags/item/`.

Goal: a spatial fracture. A pitch-black core line, surrounded by chaotic jagged purple / magenta energy that shifts over time, the UV x jittered by a hash of y and time.

```glsl
// fragment shader as supplied by the owner (core idea; keep the look, fix what the 1.21.1 pipeline needs)
#version 150
uniform sampler2D Sampler0;
uniform float GameTime;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    vec2 uv = texCoord0;
    float time = GameTime * 1500.0;
    float distortion = (hash(vec2(uv.y * 10.0, time * 0.1)) - 0.5) * 0.05;
    uv.x += distortion;
    float distFromCenter = abs(uv.x - 0.5);
    float core = smoothstep(0.05, 0.0, distFromCenter);
    float aura = smoothstep(0.15, 0.0, distFromCenter);
    vec4 voidColor = vec4(0.01, 0.01, 0.01, 1.0);
    vec4 auraColor = vec4(0.4, 0.0, 0.6, 0.8);
    vec4 baseTex = texture(Sampler0, uv);
    vec4 finalColor = mix(baseTex * vertexColor, auraColor, aura);
    finalColor = mix(finalColor, voidColor, core);
    if (baseTex.a < 0.1) discard;
    fragColor = finalColor;
}
```

Vertex shader: standard `gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0)`, passes `Color` and `UV0`. Blend: `src_alpha, one_minus_src_alpha`.
Known problem to solve: the owner's JSON declares `UV2` and `Sampler0` but the effect needs a texture whose alpha shapes the slash (a long vertical blade quad); use a generated texture.
