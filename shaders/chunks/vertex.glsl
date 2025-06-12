#version 430

struct FaceData {
    int packedFace; // 4 bytes
    int packedAOAndQuad; // 4 bytes
    int uvs [4]; // 16 bytes
};//24 bytes

struct QuadData {
    float cornerPos[12]; // 48 bytes
    vec2 uvConners[4]; // 32 bytes
};

//104 bytes total with model buffer.
//Without model buffer 24 bytes per face

layout(std430, binding = 0) restrict readonly buffer faceBuffer {
    FaceData faces[];
};

layout(std430, binding = 1) restrict readonly buffer modelBuffer {
    QuadData quads[];
};

out vec2 texels;//8 bytes
out float dist;// 4 bytes
out vec4 lightColor;// 16 bytes
out float ambient;// 4 bytes

//toal output bytes 32 bytes

uniform mat4 combined;
uniform vec3 chunkOffset;
uniform float textureWidth;
uniform float textureHeight;
uniform float sunLightIntensity;


//todo finish the uvs.
vec2 computeAtlasUVs(int index,int quadIndex, int currVertexID){
    int[] uvs = faces[index].uvs;
    float minU = uvs[0] / textureWidth;
    float minV = uvs[1] / textureHeight;
    float maxU = uvs[2] / textureWidth;
    float maxV = uvs[3] / textureHeight;
    float scaleU = maxU - minU;
    float scaleV = maxV - minV;
    vec2 uv =  quads[quadIndex].uvConners[currVertexID];
    return vec2(minU + scaleU * uv.x,minV + scaleV * uv.y);
}

void main(){
    int faceIndex = gl_VertexID >> 2;
    int vertexIndex = gl_VertexID & 3;
    int vertexOffset = vertexIndex * 3;
    int packedData = faces[faceIndex].packedFace;
    int packedAO = faces[faceIndex].packedAOAndQuad;
    int quadIndex = faces[faceIndex].packedAOAndQuad >> 8;
    int lighting = (packedData >> 15) & 65535;
    int sunlight = (lighting >> 12) & 15;
    int r = (lighting >> 8) & 15;
    int g = (lighting >> 4) & 15;
    int b = lighting & 15;

    const float factor = 0.066;
    float sunlightI = sunlight * sunLightIntensity * factor;
    lightColor = vec4(r,g,b, 1.0) * factor + sunlightI;

    vec3 blockPos = vec3(
        packedData & 31,
       (packedData >> 5) & 31,
       (packedData >> 10) & 31
    );

    float[] vertices = quads[quadIndex].cornerPos;
    //Computes the uvs.
    texels = computeAtlasUVs(faceIndex,quadIndex,vertexIndex);
    //Compute AO values
    int bitIndex = vertexIndex << 1;
    int ao = (packedAO >> bitIndex) & 3;
    ambient = (0.4 - float(ao) * 0.1) * 1.5;//1.5 is the ambient scale factor.

    vec3 vertexPos = blockPos + chunkOffset + vec3(vertices[vertexOffset],vertices[(vertexOffset + 1)],vertices[(vertexOffset + 2)]);
    gl_Position = combined * vec4(vertexPos,1.0);
    dist = length(gl_Position.xyz);
}
