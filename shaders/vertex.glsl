#version 430

layout(location = 0) in float data;
layout(location = 1) in vec2 a_texCoord0;
layout(location = 2) in int a_color;
layout(location = 3) in float ambientOcclusion;
layout(location = 4) in float sunlight;

out vec2 texels;
out float dist;
out float fog_maxdist;
out vec4 lightColor;
out float ambient;

uniform mat4 combined;
uniform vec3 chunkPos;
uniform float fogDistance;
uniform float sunLightIntensity;

void main(void) {
    //Sets the ambient Occlusion value.
    ambient = ambientOcclusion;
    //Sets the block color ambient value.
    int r = (a_color & 15);
    int g = (a_color >> 4) & 15;
    int b = (a_color >> 8) & 15;

    float ir = (r + sunlight * sunLightIntensity) /15;
    float ig = (g + sunlight * sunLightIntensity) /15;
    float ib = (b + sunlight * sunLightIntensity) /15;

    lightColor = vec4(ir,ig,ib,1.0);
    //Sets the fog distance max.
    fog_maxdist = fogDistance;
    //Sets ambient level.
    //Sets the texture.
    texels = a_texCoord0;
    //Converts the float bits to int bits.
    int packedData = floatBitsToInt(data);
    //Sets the position.
    float factor = 1024 / 33;
    float x = (packedData >> 20) / factor;
    float y = ((packedData >> 10) & 1023) / factor;
    float z = ((packedData) & (1023)) / factor;

    vec3 worldPos = chunkPos + vec3(x,y,z);

    vec4 pos = combined * vec4(worldPos,1.0);

    dist = length(pos.xyz);
    gl_Position = pos;
}

