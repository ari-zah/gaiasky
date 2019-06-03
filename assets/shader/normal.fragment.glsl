#version 120

#define TEXTURE_LOD_BIAS 0.0

////////////////////////////////////////////////////////////////////////////////////
////////// GROUND ATMOSPHERIC SCATTERING - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
varying vec4 v_atmosphereColor;

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

varying vec4 v_position;
#define pullPosition() { return v_position;}

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec4 v_color;

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_normal;
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() g_normal = v_normal

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_binormal;
vec3 g_binormal = vec3(0.0, 0.0, 1.0);
#define pullBinormal() g_binormal = v_binormal

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_tangent;
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define pullTangent() g_tangent = v_tangent

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
#define exposure 4.0

varying vec2 v_texCoord0;

// Uniforms which are always available
uniform mat4 u_projViewTrans;

uniform mat4 u_worldTrans;

uniform vec4 u_cameraPosition;

uniform mat3 u_normalMatrix;

// Varyings computed in the vertex shader
varying float v_opacity;
varying float v_alphaTest;

// Other uniforms
#ifdef shininessFlag
uniform float u_shininess;
#else
const float u_shininess = 20.0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#endif

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#if defined(emissiveTextureFlag) || defined(emissiveColorFlag)
#define specularFlag
#endif

#if defined(specularFlag) || defined(fogFlag)
#define cameraPositionFlag
#endif

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag


//////////////////////////////////////////////////////
////// SHADOW MAPPING
//////////////////////////////////////////////////////
#ifdef shadowMapFlag
#define bias 0.006
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
varying vec3 v_shadowMapUv;

float getShadowness(vec2 uv, vec2 offset, float compare){
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    return step(compare - bias, dot(texture2D(u_shadowTexture, uv + offset, TEXTURE_LOD_BIAS), bitShifts)); //+(1.0/255.0));
}


float texture2DShadowLerp(vec2 size, vec2 uv, float compare){
    vec2 texelSize = vec2(1.0) / size;
    vec2 f = fract(uv * size + 0.5);
    vec2 centroidUV = floor(uv * size + 0.5) / size;

    float lb = getShadowness(centroidUV, texelSize * vec2(0.0, 0.0), compare);
    float lt = getShadowness(centroidUV, texelSize * vec2(0.0, 1.0), compare);
    float rb = getShadowness(centroidUV, texelSize * vec2(1.0, 0.0), compare);
    float rt = getShadowness(centroidUV, texelSize * vec2(1.0, 1.0), compare);
    float a = mix(lb, lt, f.y);
    float b = mix(rb, rt, f.y);
    float c = mix(a, b, f.x);
    return c;
}

float getShadow()
{
      // Only PCF
//    float pcf = u_shadowPCFOffset / 2.0;
//    
//    return (//getShadowness(vec2(0,0)) + 
//	getShadowness(v_shadowMapUv.xy, vec2(pcf, pcf), v_shadowMapUv.z) +
//	getShadowness(v_shadowMapUv.xy, vec2(-pcf, pcf), v_shadowMapUv.z) +
//	getShadowness(v_shadowMapUv.xy, vec2(pcf, -pcf), v_shadowMapUv.z) +
//	getShadowness(v_shadowMapUv.xy, vec2(-pcf, -pcf), v_shadowMapUv.z)) * 0.25;
    
    // Complex lookup: PCF + interpolation (see http://codeflow.org/entries/2013/feb/15/soft-shadow-mapping/)
    vec2 size = vec2(1.0 / (2.0 * u_shadowPCFOffset));
    float result = 0.0;
    for(int x=-2; x<=2; x++){
        for(int y=-2; y<=2; y++){
            vec2 offset = vec2(float(x), float(y)) / size;
            //result += texture2DShadowLerp(size, v_shadowMapUv.xy + offset, v_shadowMapUv.z);
            result += getShadowness(v_shadowMapUv.xy, offset, v_shadowMapUv.z);
        }
    }
    return result / 25.0;
    
    // Simple lookup
    //return getShadowness(v_shadowMapUv.xy, vec2(0.0), v_shadowMapUv.z);
}

#endif //shadowMapFlag


// AMBIENT LIGHT
varying vec3 v_ambientLight;

// COLOR DIFFUSE
#if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
    #define fetchColorDiffuseTD(texture, texCoord, defaultValue) texture2D(texture, texCoord, TEXTURE_LOD_BIAS) * u_diffuseColor
#elif defined(diffuseTextureFlag)
    #define fetchColorDiffuseTD(texture, texCoord, defaultValue) texture2D(texture, texCoord, TEXTURE_LOD_BIAS)
#elif defined(diffuseColorFlag)
    #define fetchColorDiffuseTD(texture, texCoord, defaultValue) u_diffuseColor
#else
    #define fetchColorDiffuseTD(texture, texCoord, defaultValue) defaultValue
#endif // diffuseTextureFlag && diffuseColorFlag

#if defined(diffuseTextureFlag) || defined(diffuseColorFlag)
    #define fetchColorDiffuse(baseColor, texture, texCoord, defaultValue) baseColor * fetchColorDiffuseTD(texture, texCoord, defaultValue)
#else
    #define fetchColorDiffuse(baseColor, texture, texCoord, defaultValue) baseColor
#endif // diffuseTextureFlag || diffuseColorFlag

// COLOR EMISSIVE

#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(texture, texCoord) texture2D(texture, texCoord, TEXTURE_LOD_BIAS) * u_emissiveColor * 2.0
#elif defined(emissiveTextureFlag)
    #define fetchColorEmissiveTD(texture, texCoord) texture2D(texture, texCoord, TEXTURE_LOD_BIAS)
#elif defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(texture, texCoord) u_emissiveColor * 2.0
#endif // emissiveTextureFlag && emissiveColorFlag
    
#if defined(emissiveTextureFlag) || defined(emissiveColorFlag)
    #define fetchColorEmissive(emissiveTex, texCoord) fetchColorEmissiveTD(emissiveTex, texCoord)
#else
    #define fetchColorEmissive(emissiveTex, texCoord) vec4(0.0, 0.0, 0.0, 0.0)
#endif // emissiveTextureFlag || emissiveColorFlag

// COLOR SPECULAR

#if defined(specularTextureFlag) && defined(specularColorFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture2D(u_specularTexture, texCoord, TEXTURE_LOD_BIAS).rgb * u_specularColor.rgb
#elif defined(specularTextureFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture2D(u_specularTexture, texCoord, TEXTURE_LOD_BIAS).rgb
#elif defined(specularColorFlag)
    #define fetchColorSpecular(texCoord, defaultValue) u_specularColor.rgb
#else
    #define fetchColorSpecular(texCoord, defaultValue) defaultValue
#endif // specular


varying vec3 v_lightDir;
varying vec3 v_lightCol;
varying vec3 v_viewDir;
#ifdef environmentCubemapFlag
varying vec3 v_reflect;
#endif

#ifdef environmentCubemapFlag
uniform samplerCube u_environmentCubemap;
#endif

#ifdef reflectionColorFlag
uniform vec4 u_reflectionColor;
#endif

#define saturate(x) clamp(x, 0.0, 1.0)

#define PI 3.1415926535

void main() {
    vec2 g_texCoord0 = v_texCoord0;

    vec4 diffuse = fetchColorDiffuse(v_color, u_diffuseTexture, g_texCoord0, vec4(1.0, 1.0, 1.0, 1.0));
    vec4 emissive = fetchColorEmissive(u_emissiveTexture, g_texCoord0);
    vec3 specular = fetchColorSpecular(g_texCoord0, vec3(0.0, 0.0, 0.0));
    vec3 ambient = v_ambientLight;

    #ifdef normalTextureFlag
		vec3 N = normalize(vec3(texture2D(u_normalTexture, g_texCoord0, TEXTURE_LOD_BIAS).xyz * 2.0 - 1.0));
		#ifdef environmentCubemapFlag
			vec3 reflectDir = normalize(v_reflect + (vec3(0.0, 0.0, 1.0) - N.xyz));
		#endif // environmentCubemapFlag
    #else
	    // Normal in pixel space
	    vec3 N = vec3(0.0, 0.0, 1.0);
		#ifdef environmentCubemapFlag
			vec3 reflectDir = normalize(v_reflect);
		#endif // environmentCubemapFlag
    #endif // normalTextureFlag

    // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
    vec3 L = normalize(v_lightDir);
    vec3 V = normalize(v_viewDir);
    vec3 H = normalize(L + V);
    float NL = max(0.0, dot(N, L));
    float NH = max(0.0, dot(N, H));

    float specOpacity = 1.0; //(1.0 - diffuse.w);
    float spec = min(1.0, pow(NH, 40.0) * specOpacity);
    float selfShadow = saturate(4.0 * NL);

    #ifdef environmentCubemapFlag
		vec3 environment = textureCube(u_environmentCubemap, reflectDir).rgb;
		specular *= environment;
		#ifdef reflectionColorFlag
			diffuse.rgb = saturate(vec3(1.0) - u_reflectionColor.rgb) * diffuse.rgb + environment * u_reflectionColor.rgb;
		#endif // reflectionColorFlag
    #endif // environmentCubemapFlag

    #ifdef shadowMapFlag
    	float shdw = clamp(getShadow(), 0.05, 1.0);
        vec3 dayColor = (v_lightCol * diffuse.rgb) * NL * shdw + (ambient * diffuse.rgb) * (1.0 - NL);
        gl_FragColor = vec4(dayColor + emissive.rgb, diffuse.a * v_opacity);
    #else
        vec3 dayColor = (v_lightCol * diffuse.rgb) * NL + (ambient * diffuse.rgb) * (1.0 - NL);
        gl_FragColor = vec4(dayColor + emissive.rgb, diffuse.a * v_opacity);
    #endif // shadowMapFlag

    gl_FragColor.rgb += selfShadow * spec * specular;
    gl_FragColor.rgb += (vec3(1.0) - exp(v_atmosphereColor.rgb * -exposure)) * v_atmosphereColor.a;
    
    // Prevent saturation
    gl_FragColor = clamp(gl_FragColor, 0.0, 1.0);
    gl_FragColor.rgb *= 0.95;

    if(gl_FragColor.a == 0.0){
        discard;
    }
    


    // Debug! - vectors
    //float theta = acos(L.z); // in [0..Pi]
    //float phi = atan(L.y/L.x); // in [0..2Pi]
    //vec4 debugcol = vec4(0.0, L.y, 0.0, 1.0);
    //gl_FragColor = debugcol;

    // Debug! - visualise depth buffer
    //gl_FragColor = vec4(vec3(gl_FragCoord.z), 1.0f);
}
