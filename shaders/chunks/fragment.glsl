#version 430

layout(binding = 0) uniform sampler2D samp;

uniform float fogDistance;
uniform vec4 fogColor;

in vec2 texels;
in vec4 lightColor;
in float dist;
in float ambient;

out vec4 color;

float fogmindist =  0.3;

void main(){
   float fog_factor = (fogDistance - dist) / (fogDistance - fogmindist);
   fog_factor = clamp(fog_factor,0.0,1.0);
   //sets the ambient occlusion.
   vec4 shaderTexture = texture(samp,texels);
   //Sets the color ambient.
   vec4 combinedColor = (shaderTexture * lightColor) - ambient;
   //The final mix with fog.
   float aFactor = mix(1,fog_factor,shaderTexture.a);
   color = mix(fogColor,combinedColor,aFactor);
}

