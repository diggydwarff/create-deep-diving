// Shared by the material and fallback passes. All offsets stay inside one
// shadow-map tile, including the blocker search at the edge of the atlas.
float ddFilterTap(sampler2DShadow map, vec3 point, vec4 bounds) {
    return texture(map, vec3(clamp(point.xy, bounds.xy, bounds.zw), point.z));
}

vec2 ddDiskOffset(int index) {
    // A fixed disk avoids frame noise and swimming shadow edges.
    float angle = float(index) * 2.39996323;
    float radius = sqrt((float(index) + 0.5) / 12.0);
    return vec2(cos(angle), sin(angle)) * radius;
}

float ddFilteredShadow(sampler2DShadow map, vec3 point, vec2 texel,
                       vec2 gradient, float closeDepth, float softness, vec4 bounds) {
    if (softness <= 0.001) {
        return ddFilterTap(map, point, bounds);
    }

    float blocked = 0.0;
    float separated = 0.0;

    // Probe both the receiver and a point 0.35 blocks closer to the lamp.
    // A blocker still in front of that nearer point is separated from the
    // receiver. Let those shadows spread more than contact shadows.
    for (int index = 0; index < 4; index++) {
        float angle = float(index) * 1.57079633 + 0.78539816;
        vec2 offset = vec2(cos(angle), sin(angle)) * texel * 3.5 * softness;
        float correction = dot(gradient, offset);
        blocked += 1.0 - ddFilterTap(map, point + vec3(offset, correction), bounds);
        separated += 1.0 - ddFilterTap(map, vec3(point.xy + offset, closeDepth + correction), bounds);
    }

    float spread = clamp(separated / max(blocked, 0.001), 0.0, 1.0);
    float radius = (1.0 + 2.5 * spread) * softness;
    float visible = 0.0;

    for (int index = 0; index < 12; index++) {
        vec2 offset = ddDiskOffset(index) * texel * radius;
        visible += ddFilterTap(map, point + vec3(offset, dot(gradient, offset)), bounds);
    }

    return visible / 12.0;
}
