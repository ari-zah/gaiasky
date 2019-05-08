#version 330 core

#define TEXTURE_LOD_BIAS 0.2

////////////////////////////////////////////////////////////////////////////////////
////////// GROUND ATMOSPHERIC SCATTERING - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
in vec4 v_atmosphereColor;

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

in vec4 v_position;
#define pullPosition() { return v_position;}

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec4 v_color;

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec3 v_normal;
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() g_normal = v_normal

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec3 v_binormal;
vec3 g_binormal = vec3(0.0, 0.0, 1.0);
#define pullBinormal() g_binormal = v_binormal

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec3 v_tangent;
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define pullTangent() g_tangent = v_tangent

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
#define exposure 4.0

in vec2 v_texCoord0;

// Uniforms which are always available
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;

// Varyings computed in the vertex shader
in float v_opacity;
in float v_alphaTest;

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

#ifdef reflectionTextureFlag
uniform sampler2D u_reflectionTexture;
#endif

#ifdef heightTextureFlag
uniform sampler2D u_heightTexture;
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

#if defined(heightTextureFlag)
#define heightFlag
#endif //heightFlag


//////////////////////////////////////////////////////
////// SHADOW MAPPING
//////////////////////////////////////////////////////
#ifdef shadowMapFlag
#define bias 0.006
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
in vec3 v_shadowMapUv;

float getShadowness(vec2 uv, vec2 offset, float compare){
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    return step(compare - bias, dot(texture(u_shadowTexture, uv + offset, TEXTURE_LOD_BIAS), bitShifts)); //+(1.0/255.0));
}


float textureShadowLerp(vec2 size, vec2 uv, float compare){
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
            //result += textureShadowLerp(size, v_shadowMapUv.xy + offset, v_shadowMapUv.z);
            result += getShadowness(v_shadowMapUv.xy, offset, v_shadowMapUv.z);
        }
    }
    return result / 25.0;
    
    // Simple lookup
    //return getShadowness(v_shadowMapUv.xy, vec2(0.0), v_shadowMapUv.z);
}

#endif //shadowMapFlag


// AMBIENT LIGHT
in vec3 v_ambientLight;

// COLOR DIFFUSE
#if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
    #define fetchColorDiffuseTD(tex, texCoord, defaultValue) texture(tex, texCoord, TEXTURE_LOD_BIAS) * u_diffuseColor
#elif defined(diffuseTextureFlag)
    #define fetchColorDiffuseTD(tex, texCoord, defaultValue) texture(tex, texCoord, TEXTURE_LOD_BIAS)
#elif defined(diffuseColorFlag)
    #define fetchColorDiffuseTD(tex, texCoord, defaultValue) u_diffuseColor
#else
    #define fetchColorDiffuseTD(tex, texCoord, defaultValue) defaultValue
#endif // diffuseTextureFlag && diffuseColorFlag

#if defined(diffuseTextureFlag) || defined(diffuseColorFlag)
    #define fetchColorDiffuse(baseColor, tex, texCoord, defaultValue) baseColor * fetchColorDiffuseTD(tex, texCoord, defaultValue)
#else
    #define fetchColorDiffuse(baseColor, tex, texCoord, defaultValue) baseColor
#endif // diffuseTextureFlag || diffuseColorFlag

// COLOR EMISSIVE

#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord, TEXTURE_LOD_BIAS) * u_emissiveColor * 2.0
#elif defined(emissiveTextureFlag)
    #define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord, TEXTURE_LOD_BIAS)
#elif defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(tex, texCoord) u_emissiveColor * 2.0
#endif // emissiveTextureFlag && emissiveColorFlag
    
#if defined(emissiveTextureFlag) || defined(emissiveColorFlag)
    #define fetchColorEmissive(emissiveTex, texCoord) fetchColorEmissiveTD(emissiveTex, texCoord)
#else
    #define fetchColorEmissive(emissiveTex, texCoord) vec4(0.0, 0.0, 0.0, 0.0)
#endif // emissiveTextureFlag || emissiveColorFlag

// COLOR SPECULAR

#if defined(specularTextureFlag) && defined(specularColorFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture(u_specularTexture, texCoord, TEXTURE_LOD_BIAS).rgb * u_specularColor.rgb
#elif defined(specularTextureFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture(u_specularTexture, texCoord, TEXTURE_LOD_BIAS).rgb
#elif defined(specularColorFlag)
    #define fetchColorSpecular(texCoord, defaultValue) u_specularColor.rgb
#else
    #define fetchColorSpecular(texCoord, defaultValue) defaultValue
#endif // specular


in vec3 v_lightDir;
in vec3 v_lightCol;
in vec3 v_viewDir;
in float v_depth;

////////////////////////////////
//  PARALLAX MAPPING
////////////////////////////////
#ifdef heightFlag
in vec3 v_tangentFragPos;
in vec3 v_tangentViewPos;
#endif // heightFlag

#ifdef environmentCubemapFlag
in vec3 v_reflect;
#endif

#ifdef environmentCubemapFlag
uniform samplerCube u_environmentCubemap;
#endif

#ifdef reflectionColorFlag
uniform vec4 u_reflectionColor;
#endif

out vec4 fragColor;

#define saturate(x) clamp(x, 0.0, 1.0)

#define PI 3.1415926535

#ifdef heightFlag
#define HEIGHT_SCALE 0.02
vec2 parallaxMapping(vec2 texCoords, vec3 viewDir){
    // number of depth layers
    const float minLayers = 8;
    const float maxLayers = 32;
    float numLayers = mix(maxLayers, minLayers, abs(dot(vec3(0.0, 0.0, 1.0), viewDir)));
    // calculate the size of each layer
    float layerDepth = 1.0 / numLayers;
    // depth of current layer
    float currentLayerDepth = 0.0;

    // the amount to shift the texture coordinates per layer (from vector P)
    vec2 P = viewDir.xy / viewDir.z * HEIGHT_SCALE;
    vec2 deltaTexCoords = P / numLayers;

    // get initial values
    vec2  currentTexCoords     = texCoords;
    float currentDepthMapValue = texture(u_heightTexture, currentTexCoords, TEXTURE_LOD_BIAS).r;

    while(currentLayerDepth < currentDepthMapValue){
        // shift texture coordinates along direction of P
        currentTexCoords -= deltaTexCoords;
        // get depthmap value at current texture coordinates
        currentDepthMapValue = texture(u_heightTexture, currentTexCoords, TEXTURE_LOD_BIAS).r;
        // get depth of next layer
        currentLayerDepth += layerDepth;
    }

    // get texture coordinates before collision (reverse operations)
    vec2 prevTexCoords = currentTexCoords + deltaTexCoords;

    // get depth after and before collision for linear interpolation
    float afterDepth  = currentDepthMapValue - currentLayerDepth;
    float beforeDepth = texture(u_heightTexture, prevTexCoords, TEXTURE_LOD_BIAS).r - currentLayerDepth + layerDepth;

    // interpolation of texture coordinates
    float weight = afterDepth / (afterDepth - beforeDepth);
    vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0 - weight);

    return finalTexCoords;
}

vec2 parallaxMappingSimple(vec2 texCoords, vec3 viewDir){
    float height = texture(u_heightTexture, texCoords).r;
    vec3 displacement = viewDir * (height * HEIGHT_SCALE);
    vec2 p = displacement.xy;
    return texCoords - p;
}
#endif // heightFlag


void main() {
    vec2 texCoords = v_texCoord0;

    // Parallax occlusion mapping
    #ifdef heightFlag
    vec3 tfp = v_tangentFragPos;
    vec3 tvp = v_tangentViewPos;
    vec3 viewDir = normalize(tvp - tfp);
    texCoords = parallaxMappingSimple(texCoords, viewDir);
    #endif // heightFlag

    vec4 diffuse = fetchColorDiffuse(v_color, u_diffuseTexture, texCoords, vec4(1.0, 1.0, 1.0, 1.0));
    vec4 emissive = fetchColorEmissive(u_emissiveTexture, texCoords);
    vec3 specular = fetchColorSpecular(texCoords, vec3(0.0, 0.0, 0.0));
    vec3 ambient = v_ambientLight;

    #ifdef normalTextureFlag
		vec3 N = normalize(vec3(texture(u_normalTexture, texCoords, TEXTURE_LOD_BIAS).xyz * 2.0 - 1.0));
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
		vec3 environment = texture(u_environmentCubemap, reflectDir).rgb;
		specular *= environment;
		#ifdef reflectionColorFlag
			diffuse.rgb = saturate(vec3(1.0) - u_reflectionColor.rgb) * diffuse.rgb + environment * u_reflectionColor.rgb;
		#endif // reflectionColorFlag
    #endif // environmentCubemapFlag

    #ifdef shadowMapFlag
    	float shdw = clamp(getShadow(), 0.05, 1.0);
        vec3 dayColor = (v_lightCol * diffuse.rgb) * NL * shdw + (ambient * diffuse.rgb) * (1.0 - NL);
        fragColor = vec4(dayColor + emissive.rgb, diffuse.a * v_opacity);
    #else
        vec3 dayColor = (v_lightCol * diffuse.rgb) * NL + (ambient * diffuse.rgb) * (1.0 - NL);
        fragColor = vec4(dayColor + emissive.rgb, diffuse.a * v_opacity);
    #endif // shadowMapFlag

    fragColor.rgb += selfShadow * spec * specular;
    fragColor.rgb += (vec3(1.0) - exp(v_atmosphereColor.rgb * -exposure)) * v_atmosphereColor.a;
    
    // Prevent saturation
    fragColor = clamp(fragColor, 0.0, 1.0);

    if(fragColor.a == 0.0){
        discard;
    }

    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
