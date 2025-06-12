#version 430

layout(location = 0) in vec3 mesh_pos;
layout(location = 1) in vec2 mesh_texture;
layout(location = 2) in float mesh_normal;

out float normal;
out vec2 texels;
uniform mat4 projection;
uniform mat4 modelView;

void main(void){
    normal = mesh_normal;
    texels = mesh_texture;
    gl_Position = projection * modelView * vec4(mesh_pos,1.0);
}