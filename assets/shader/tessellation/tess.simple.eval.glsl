#version 410 core

layout (triangles) in;

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
#include shader/lib_geometry.glsl
#include shader/lib_relativity.glsl
#endif // relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif // gravitationalWaves


uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

uniform float u_heightScale;
uniform float u_heightNoiseSize;
uniform vec2 u_heightSize;
uniform sampler2D u_heightTexture;
uniform sampler2D u_normalTexture;


in vec2 l_texCoords[gl_MaxPatchVertices];
in vec3 l_normal[gl_MaxPatchVertices];

#include shader/lib_sampleheight.glsl

void main(void){
    vec4 pos = (gl_TessCoord.x * gl_in[0].gl_Position +
                    gl_TessCoord.y * gl_in[1].gl_Position +
                    gl_TessCoord.z * gl_in[2].gl_Position);

    vec2 v_texCoords = (gl_TessCoord.x * l_texCoords[0] + gl_TessCoord.y * l_texCoords[1] + gl_TessCoord.z * l_texCoords[2]);

    // Normal to apply height
    vec3 v_normal = normalize(gl_TessCoord.x * l_normal[0] + gl_TessCoord.y * l_normal[1] + gl_TessCoord.z * l_normal[2]);

    // Use height texture to move vertex along normal
    float h = 1.0 - sampleHeight(u_heightTexture, v_texCoords).r;
    vec3 dh = v_normal * h * u_heightScale;
    pos += vec4(dh, 0.0);


    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    gl_Position = u_projViewTrans * pos;
}
