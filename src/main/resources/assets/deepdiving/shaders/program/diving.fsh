#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Hypoxia;
uniform float Narcosis;
uniform float Toxicity;
uniform float Decompression;
uniform float Pressure;
uniform float Shock;
uniform float Clock;
uniform float Motion;
uniform float CenterCalm;
uniform float Quality;
uniform float Blood;

in vec2 texCoord;
out vec4 fragColor;

// Clamp every offset sample to half a texel: distortion never pulls in black borders.
vec3 scene(vec2 uv) {
    vec2 edge = 0.5 / InSize;
    return texture(DiffuseSampler, clamp(uv, edge, vec2(1.0) - edge)).rgb;
}

void main() {
    vec2 uv = texCoord;
    vec2 position = (uv - 0.5) * vec2(InSize.x / InSize.y, 1.0);
    float peripheral = smoothstep(0.10, 0.75, length(position));
    float sensitivity = mix(1.0, peripheral, CenterCalm);
    float t = Clock * Motion;
    float n = Narcosis;
    float injury = min(1.0, Pressure + Shock * 0.65);

    // Slow optical drift for narcosis; a short radial pinch for pressure exposure.
    vec2 drift = vec2(sin(position.y * 5.0 + t * 0.65),
                      sin(position.x * 4.3 - t * 0.48));
    uv += drift * (0.0015 + 0.008 * n) * n * sensitivity * Motion;
    uv += (texCoord - 0.5) * injury * 0.016 * sensitivity;
    uv += vec2(sin(t * 8.7), cos(t * 7.3)) * Shock * 0.003 * Motion;

    // Focus comes and goes gently. Reduced motion keeps the optical impairment steady.
    float toxicPulse = mix(0.65, 0.5 + 0.5 * sin(t * 2.1), Motion);
    float decoPulse = mix(0.65, 0.3 + 0.7 * abs(sin(t * 0.73) * sin(t * 1.31)), Motion);
    float blur = (Hypoxia * 4.5 + n * 1.5 + Toxicity * toxicPulse * 3.8
                  + Decompression * decoPulse * 3.0 + injury * 0.8) * sensitivity;
    vec2 stepSize = vec2(blur) / InSize;
    vec3 colour;
    if (Quality > 0.5) {
        colour = scene(uv) * 0.25;
        colour += (scene(uv + vec2(stepSize.x, 0.0)) + scene(uv - vec2(stepSize.x, 0.0))
                 + scene(uv + vec2(0.0, stepSize.y)) + scene(uv - vec2(0.0, stepSize.y))) * 0.125;
        colour += (scene(uv + stepSize) + scene(uv - stepSize)
                 + scene(uv + vec2(stepSize.x, -stepSize.y))
                 + scene(uv + vec2(-stepSize.x, stepSize.y))) * 0.0625;
    } else {
        colour = scene(uv) * 0.5;
        colour += (scene(uv + vec2(stepSize.x, 0.0)) + scene(uv - vec2(stepSize.x, 0.0))
                 + scene(uv + vec2(0.0, stepSize.y)) + scene(uv - vec2(0.0, stepSize.y))) * 0.125;
    }

    // Spatial double vision needs no retained frames, so turns and teleports cannot leave trails.
    vec2 doubleOffset = vec2(0.008, 0.0025) * n * sensitivity;
    doubleOffset *= 1.0 + 0.2 * sin(t * 0.55) * Motion;
    colour = mix(colour, scene(uv + doubleOffset), n * 0.24 * sensitivity);
    if (Quality > 0.5) {
        vec2 separation = vec2(0.0018, 0.0003) * injury * sensitivity;
        colour.r = mix(colour.r, scene(uv + separation).r, injury * 0.45);
        colour.b = mix(colour.b, scene(uv - separation).b, injury * 0.45);
    }

    float luminance = dot(colour, vec3(0.2126, 0.7152, 0.0722));
    float desaturation = min(0.88, Hypoxia * 0.78 + Toxicity * 0.12 + Decompression * 0.08);
    colour = mix(colour, vec3(luminance), desaturation);
    colour *= 1.0 - Hypoxia * 0.12 * peripheral;
    colour = mix(colour, colour * vec3(1.12, 0.76, 0.78), injury * peripheral * 0.28 * Blood);
    // Oxygen toxicity slightly alters contrast rather than using full-screen flashes.
    colour = mix(colour, (colour - 0.5) * 1.08 + 0.5, Toxicity * toxicPulse * 0.4);
    fragColor = vec4(clamp(colour, 0.0, 1.0), 1.0);
}
