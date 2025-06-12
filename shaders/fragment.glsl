#version 430

layout(binding = 0) uniform sampler2D samp;

in vec2 texels;
in vec4 lightColor;
in float dist;
in float fog_maxdist;
in float ambient;

out vec4 color;

float fogmindist =  0.1;
vec4 fog_color = vec4(0.5, 0.6, 0.6, 1.0);

void main(void){

    float fog_factor = (fog_maxdist - dist) / (fog_maxdist - fogmindist);
    fog_factor = clamp(fog_factor,0.0,1.0);
    //sets the ambient occlusion.
    vec4 shaderTexture = texture(samp,texels);
    //Sets the color ambient.
    vec4 finalShader = ((shaderTexture * lightColor) * ambient);
    //The final mix with fog.
    color = mix(fog_color,finalShader,fog_factor);
}