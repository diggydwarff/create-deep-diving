uniform sampler2D SceneColorTex;

// The upstream reconstruction is declared below the injected helpers.
vec3 reconstructWorld(vec2 texCoord, float depth);

vec3 surfacePoint(vec2 coordinate) {
    return reconstructWorld(coordinate, texture(DepthTex, coordinate).r);
}

vec3 surfaceNormal(vec2 coordinate, vec3 point) {
    vec2 texel = 1.0 / vec2(textureSize(DepthTex, 0));

    vec3 left = point - surfacePoint(coordinate - vec2(texel.x, 0.0));
    vec3 right = surfacePoint(coordinate + vec2(texel.x, 0.0)) - point;
    vec3 down = point - surfacePoint(coordinate - vec2(0.0, texel.y));
    vec3 up = surfacePoint(coordinate + vec2(0.0, texel.y)) - point;

    // Pick the neighbor on the same face. Screen derivatives straddle depth
    // edges and produce spurious normals: the one-pixel black block outlines.
    vec3 dx = dot(left, left) < dot(right, right) ? left : right;
    vec3 dy = dot(down, down) < dot(up, up) ? down : up;
    vec3 normal = cross(dx, dy);

    if (dot(normal, normal) < 0.000000000001) {
        return normalize(CameraPos - point);
    }

    normal = normalize(normal);
    return dot(normal, CameraPos - point) < 0.0 ? -normal : normal;
}

float surfaceShadow(vec3 point, vec3 normal, float ndotl) {
    // A small normal offset follows the shadow texel's world footprint. Keep
    // it bounded so thin terrain remains an obstacle instead of leaking light.
    float footprint = distance(point, ShadowOrigin) * TanOuter * ShadowTexel.x;
    float bias = 0.004 + min(0.018, footprint * (1.0 - ndotl));
    vec3 receiver = point + normal * bias;
    vec4 clip = LightViewProj * vec4(receiver - ShadowOrigin, 1.0);

    if (clip.w <= 0.000001) {
        return 0.0;
    }

    vec3 samplePoint = clip.xyz / clip.w * 0.5 + 0.5;

    if (any(lessThanEqual(samplePoint, vec3(0.0))) || any(greaterThanEqual(samplePoint, vec3(1.0)))) {
        return 0.0;
    }

    vec2 stepSize = ShadowTexel * 0.85;

    // Compare each tap with the receiver plane at that tap. A shared depth
    // reference falsely shadows sloping faces, even when nothing blocks them.
    vec3 axis = abs(normal.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent = normalize(cross(normal, axis));
    vec4 horizontal = LightViewProj * vec4(tangent, 0.0);
    vec4 vertical = LightViewProj * vec4(cross(normal, tangent), 0.0);
    vec3 dx = (horizontal.xyz * clip.w - clip.xyz * horizontal.w) / (clip.w * clip.w);
    vec3 dy = (vertical.xyz * clip.w - clip.xyz * vertical.w) / (clip.w * clip.w);
    vec3 plane = cross(dx, dy);
    vec2 gradient = abs(plane.z) > 0.00000001 ? -plane.xy / plane.z : vec2(0.0);
    gradient = clamp(gradient, vec2(-0.1), vec2(0.1));

    vec3 horizontalStep = vec3(stepSize.x, 0.0, gradient.x * stepSize.x);
    vec3 verticalStep = vec3(0.0, stepSize.y, gradient.y * stepSize.y);
    float visible = texture(ShadowTex, samplePoint) * 0.4;
    visible += texture(ShadowTex, samplePoint + horizontalStep) * 0.15;
    visible += texture(ShadowTex, samplePoint - horizontalStep) * 0.15;
    visible += texture(ShadowTex, samplePoint + verticalStep) * 0.15;
    visible += texture(ShadowTex, samplePoint - verticalStep) * 0.15;
    return visible;
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

vec3 surfaceResponse(vec2 coordinate, vec3 point) {
    vec3 color = max(texture(SceneColorTex, coordinate).rgb, vec3(0.0));
    vec2 texel = 2.0 / vec2(textureSize(SceneColorTex, 0));
    float center = luminance(color);
    float total = center;
    float count = 1.0;

    // Estimate the local exposure without crossing foreground silhouettes.
    // Apply the same bounded gain to RGB so the existing block texture, color
    // and shader shading remain together rather than becoming a white overlay.
    for (int index = 0; index < 4; index++) {
        vec2 offset = index < 2 ? vec2(index == 0 ? texel.x : -texel.x, 0.0)
                               : vec2(0.0, index == 2 ? texel.y : -texel.y);
        vec2 neighbor = coordinate + offset;
        float gap = distance(surfacePoint(neighbor), point);
        float weight = 1.0 - smoothstep(0.04, 0.16, gap);
        total += luminance(max(texture(SceneColorTex, neighbor).rgb, vec3(0.0))) * weight;
        count += weight;
    }

    float average = total / count;
    float gain = clamp(0.65 / max(average, 0.015), 1.0, 18.0);
    vec3 reflected = clamp(color * gain, vec3(0.0), vec3(1.0));

    // A post-pass cannot recover a texture already rendered completely black.
    // Keep a faint fallback there, without filling ordinary textured surfaces.
    float recoverable = smoothstep(0.001, 0.010, average);
    float brightest = max(color.r, max(color.g, color.b));
    float headroom = 1.0 - smoothstep(0.45, 1.0, brightest);
    return mix(vec3(0.16), reflected, recoverable) * headroom;
}
