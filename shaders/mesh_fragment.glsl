#version 430

layout(binding = 0) uniform sampler2D samp;

in float normal;
in vec2 texels;
out vec4 color;


void main(void){
    color = texture(samp,texels) * normal;
}