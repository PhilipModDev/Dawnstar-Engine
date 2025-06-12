#version 430

layout(location = 0) in vec3 sky_pos;

uniform float sunlightIntensity;
uniform mat4 combined;
uniform vec3 playerPos;
uniform vec3 sky_color;
out vec4 skyColor;

void main(){
    skyColor = vec4(sky_color,1.0) * sunlightIntensity;
    vec3 offset = (sky_pos * 100) + playerPos;
    vec4 pos = combined * vec4(offset,1.0f);
    gl_Position = vec4(pos.x,pos.y,pos.w,pos.w);
}