// Native material lighting: no final-scene color or ambient-light dependency.
// Positions and shadow origins are relative to this frame's camera.
uniform int ddLampCount;
uniform sampler2DShadow ddLampShadow;
uniform sampler3D ddLampMedium;
uniform vec3 ddMediumOrigin;
uniform vec3 ddMediumSize;
uniform float ddOutsideWater;
uniform float ddShadowSoftness;

vec2 ddMediumAt(vec3 point) {
    vec3 cell = point - ddMediumOrigin;
    vec3 coordinate = cell / ddMediumSize;
    if (any(lessThan(coordinate, vec3(0.0))) || any(greaterThanEqual(coordinate, vec3(1.0)))) {
        return vec2(ddOutsideWater, 0.0);
    }
    vec2 medium = texture(ddLampMedium, coordinate).rg;
    float wet = medium.r > 0.0 ? step(fract(cell.y), medium.r) : 0.0;
    return vec2(wet, medium.g);
}

float ddLampTransmission(vec3 origin, vec3 point) {
    vec3 path = point - origin;
    float segment = length(path) / 8.0;
    float opticalDepth = 0.0;
    for (int index = 0; index < 8; index++) {
        vec2 medium = ddMediumAt(origin + path * ((float(index) + 0.5) / 8.0));
        opticalDepth += medium.x * (0.012 + medium.y * 0.38) * segment;
    }
    // The pack applies the receiver-to-eye fog later. Do not apply it twice.
    return exp(-opticalDepth);
}

float ddVisibility(vec3 point, vec3 normal, vec3 origin, mat4 matrix, float tangent, int slot) {
    float footprint = distance(point, origin) * tangent / 512.0;
    vec3 receiver = point + normal * (0.004 + min(0.018, footprint));
    vec4 clip = matrix * vec4(receiver - origin, 1.0);
    if (clip.w <= 0.000001) return 0.0;
    vec3 samplePoint = clip.xyz / clip.w * 0.5 + 0.5;
    if (any(lessThanEqual(samplePoint, vec3(0.0))) || any(greaterThanEqual(samplePoint, vec3(1.0)))) return 0.0;

    // Receiver-plane correction prevents a sloped block shadowing itself.
    vec3 axis = abs(normal.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangentAxis = normalize(cross(normal, axis));
    vec4 horizontal = matrix * vec4(tangentAxis, 0.0);
    vec4 vertical = matrix * vec4(cross(normal, tangentAxis), 0.0);
    vec3 dx = (horizontal.xyz * clip.w - clip.xyz * horizontal.w) / (clip.w * clip.w);
    vec3 dy = (vertical.xyz * clip.w - clip.xyz * vertical.w) / (clip.w * clip.w);
    vec3 plane = cross(dx, dy);
    vec2 gradient = abs(plane.z) > 0.00000001 ? clamp(-plane.xy / plane.z, vec2(-0.1), vec2(0.1)) : vec2(0.0);
    float distanceToLight = max(0.001, distance(receiver, origin));
    vec3 nearer = mix(receiver, origin, min(0.35 / distanceToLight, 0.5));
    vec4 nearClip = matrix * vec4(nearer - origin, 1.0);
    float closeDepth = nearClip.z / nearClip.w * 0.5 + 0.5;

    vec2 tile = vec2(float(slot - (slot / 4) * 4), float(slot / 4));
    vec2 atlasSize = vec2(4.0, 2.0);
    vec2 inset = vec2(0.5 / 512.0);
    vec4 bounds = vec4((tile + inset) / atlasSize, (tile + 1.0 - inset) / atlasSize);
    vec3 atlasPoint = vec3((tile + samplePoint.xy) / atlasSize, samplePoint.z);

    return ddFilteredShadow(ddLampShadow, atlasPoint, vec2(1.0 / 512.0) / atlasSize,
        gradient * atlasSize, closeDepth, ddShadowSoftness, bounds);
}

vec3 ddOneLamp(vec3 point, vec3 normal, int slot, vec4 position, vec4 forward,
               vec4 right, vec4 color, vec4 shadowOrigin, mat4 matrix) {
    vec3 relative = point - position.xyz;
    float axial = dot(relative, forward.xyz);
    float distanceAlong = axial - shadowOrigin.w;
    if (distanceAlong <= 0.0 || distanceAlong >= position.w) return vec3(0.0);

    vec3 up = normalize(cross(right.xyz, forward.xyz));
    float radius = length(vec2(dot(relative, right.xyz), dot(relative, up)));
    float radial = radius / max(0.0001, axial * forward.w);
    if (radial >= 1.0) return vec3(0.0);

    float inner = radius / max(0.0001, axial * right.w);
    float profile = (exp(-inner * inner * 1.55) * 1.30 + exp(-radial * radial * 2.75) * 0.60)
        * (1.0 - smoothstep(0.55, 1.0, radial));
    float endFade = 1.0 - smoothstep(position.w * 0.65, position.w, distanceAlong);
    float falloff = 1.0 / (1.0 + 0.030 * distanceAlong + 0.0032 * distanceAlong * distanceAlong);

    normal = normalize(normal);
    vec3 toLight = normalize(position.xyz - point);
    if (dot(normal, toLight) < 0.0) normal = -normal;
    float grazing = mix(0.60, 1.0, max(0.0, dot(normal, toLight)));
    float visible = ddVisibility(point, normal, shadowOrigin.xyz, matrix, forward.w, slot);
    return color.rgb * color.w * profile * falloff * endFade * grazing * visible
        * ddLampTransmission(position.xyz, point);
}
