#version 430

layout(location = 0) in vec3 a_position;
layout(location = 1) in vec2 a_texCoord0;
layout(location = 2) in mat4 aMV;


out vec2 texels;

uniform mat4 projection;
uniform mat4 skyBoxMatrix;
uniform bool useInstanceRendering;

mat4 computeModelViewMatrix(bool useInstanceRendering, mat4 aMV, mat4 skyBoxMatrix) {
    if (useInstanceRendering) {
        return skyBoxMatrix * aMV;
    }
    return skyBoxMatrix;
}

void main(){
    texels = a_texCoord0;
    mat4 mv = computeModelViewMatrix(useInstanceRendering,aMV, skyBoxMatrix);
    vec4 pos = projection * mv * vec4(a_position,1.0f);
    gl_Position = pos;
}