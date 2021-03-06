#version 330 core

in vec4 a_position;
in vec4 a_color;

uniform mat4 u_worldTransform;
uniform mat4 u_projView;
uniform vec3 u_parentPos;
uniform float u_pointSize;
uniform float u_vrScale;

out vec4 v_col;

#ifdef relativisticEffects
#include shader/lib_geometry.glsl
#include shader/lib_relativity.glsl
#endif// relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
    vec4 pos = a_position;

    pos.xyz -= u_parentPos;
    pos = u_worldTransform * pos;

    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    gl_PointSize = u_pointSize;
    v_col = a_color;

    // Position
    vec4 gpos = u_projView * pos;
    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, pos);
    #endif
}
