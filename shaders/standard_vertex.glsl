#version 430

layout(location = 0) in vec3 a_position;
layout(location = 1) in vec2 a_texCoord0;

out vec2 texels;
uniform mat4 projection;
uniform mat4 modelView;

void main(){
    texels = a_texCoord0;
    gl_Position = projection * modelView * vec4(a_position,1.0);
}