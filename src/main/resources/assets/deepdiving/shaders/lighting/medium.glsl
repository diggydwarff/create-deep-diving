// These functions extend Illumination API's MIT spotlight shader at load time.
// All positions are relative to the camera, avoiding distant-world float jitter.
uniform sampler3D MediumTex;
uniform sampler2D FrontDepthTex;
uniform vec3 MediumOrigin;
uniform vec3 MediumSize;
uniform vec3 ShadowOrigin;
uniform float WaterExtinction;
uniform float WaterFogStart;
uniform float WaterFogEnd;
uniform float OutsideWater;
uniform float MaskHand;

vec2 mediumAt(vec3 point) {
    vec3 cell = point - MediumOrigin;
    vec3 coordinate = cell / MediumSize;
    if (any(lessThan(coordinate, vec3(0.0))) || any(greaterThanEqual(coordinate, vec3(1.0)))) {
        return vec2(OutsideWater, 0.0);
    }
    vec2 medium = texture(MediumTex, coordinate).rg;
    // The red channel stores actual fluid height, so the partly filled surface
    // cell does not become a whole block of water when the player surfaces.
    float wet = step(fract(cell.y), medium.r);
    return vec2(medium.r > 0.0 ? wet : 0.0, medium.g);
}

// Optical depth and wet distance are accumulated only inside water cells.
// Air pockets therefore restore dry acoustics/appearance without changing sky light.
vec2 waterPath(vec3 start, vec3 finish, int samples) {
    vec3 delta = finish - start;
    float lengthInWorld = length(delta);
    float segment = lengthInWorld / float(samples);
    float opticalDepth = 0.0;
    float wetDistance = 0.0;
    for (int index = 0; index < samples; index++) {
        vec2 medium = mediumAt(start + delta * ((float(index) + 0.5) / float(samples)));
        wetDistance += medium.x * segment;
        opticalDepth += medium.x * (WaterExtinction + medium.y * 0.38) * segment;
    }
    return vec2(opticalDepth, wetDistance);
}

float lampTransmission(vec3 start, vec3 finish) {
    return exp(-waterPath(start, finish, 8).x);
}

float waterFog(float wetDistance) {
    float end = max(0.01, WaterFogEnd);
    float start = clamp(WaterFogStart, 0.0, max(0.0, end - 0.01));
    return 1.0 - smoothstep(start, end, wetDistance);
}

float viewTransmission(vec3 point) {
    vec2 path = waterPath(CameraPos, point, 8);
    return exp(-path.x) * waterFog(path.y);
}

// Volume marching already samples the beam repeatedly. Fewer path samples keep
// that work bounded without reducing the surface/air-pocket sampling accuracy.
float volumeTransmission(vec3 point) {
    vec2 path = waterPath(CameraPos, point, 4);
    return exp(-path.x - waterPath(LightPos, point, 4).x) * waterFog(path.y);
}
