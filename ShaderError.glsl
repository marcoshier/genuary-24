#version 460 core
#define OR_IN_OUT
#define OR_GL

// <primitive-types> (ShadeStyleGLSL.kt)
#define d_vertex_buffer 0
#define d_image 1
#define d_circle 2
#define d_rectangle 3
#define d_font_image_map 4
#define d_expansion 5
#define d_fast_line 6
#define d_mesh_line 7
#define d_point 8
#define d_custom 9
#define d_primitive d_rectangle
// </primitive-types>



uniform mat4 p_viewMatrix;
uniform vec3 p_origin;
uniform float p_time;
#ifdef OR_GL    
layout(origin_upper_left) in vec4 gl_FragCoord;
#endif   


// <drawer-uniforms(true, false)> (ShadeStyleGLSL.kt)
            
layout(shared) uniform ContextBlock {
    uniform mat4 u_modelNormalMatrix;
    uniform mat4 u_modelMatrix;
    uniform mat4 u_viewNormalMatrix;
    uniform mat4 u_viewMatrix;
    uniform mat4 u_projectionMatrix;
    uniform float u_contentScale;
    uniform float u_modelViewScalingFactor;
    uniform vec2 u_viewDimensions;
};
            
// </drawer-uniforms>
in vec3 va_position;
in vec3 va_normal;
in vec2 va_texCoord0;
in vec3 vi_offset;
in vec2 vi_dimensions;
in float vi_rotation;
in vec4 vi_fill;
in vec4 vi_stroke;
in float vi_strokeWeight;


// <transform-varying-in> (ShadeStyleGLSL.kt)
in vec3 v_worldNormal;
in vec3 v_viewNormal;
in vec3 v_worldPosition;
in vec3 v_viewPosition;
in vec4 v_clipPosition;
flat in mat4 v_modelNormalMatrix;
// </transform-varying-in>

out vec4 o_color;

vec3 opRepeatInfinite_2911168863(vec3 x__, vec3 y__) { 
    float rX = (x__.x - (round((x__.x / y__.x)) * y__.x));
    float rY = (x__.y - (round((x__.y / y__.y)) * y__.y));
    float rZ = (x__.z - (round((x__.z / y__.z)) * y__.z));                    
    return vec3(rX, rY, rZ);
}
#ifndef f_rotateX_4006468443
#define f_rotateX_4006468443
mat3 rotateX_4006468443(float x__) { 
                    
    return mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, cos(x__), (-sin(x__))), vec3(0.0, sin(x__), cos(x__)));
}
#endif
#ifndef f_rotateY_3578297826
#define f_rotateY_3578297826
mat3 rotateY_3578297826(float x__) { 
                    
    return mat3(vec3(cos(x__), 0.0, (-sin(x__))), vec3(0.0, 1.0, 0.0), vec3(sin(x__), 0.0, cos(x__)));
}
#endif
#ifndef f_rotateZ_209996671
#define f_rotateZ_209996671
mat3 rotateZ_209996671(float x__) { 
                    
    return mat3(vec3(cos(x__), (-sin(x__)), 0.0), vec3(sin(x__), cos(x__), 0.0), vec3(0.0, 0.0, 1.0));
}
#endif
float sdBoxFrame( vec3 p, vec3 b, float e ) {
       p = abs(p  )-b;
  vec3 q = abs(p+e)-e;
  return min(min(
      length(max(vec3(p.x,q.y,q.z),0.0))+min(max(p.x,max(q.y,q.z)),0.0),
      length(max(vec3(q.x,p.y,q.z),0.0))+min(max(q.x,max(p.y,q.z)),0.0)),
      length(max(vec3(q.x,q.y,p.z),0.0))+min(max(q.x,max(q.y,p.z)),0.0));
}

float opSmoothUnion( float d1, float d2, float k ) {
    float h = clamp( 0.5 + 0.5*(d2-d1)/k, 0.0, 1.0 );
    return mix( d2, d1, h ) - k*h*(1.0-h); }


float sdSphere(vec3 p, float s) {
  return length(p)-s;
}
#ifndef f_scene_1411025084
#define f_scene_1411025084
float scene_1411025084(vec3 x__) { 
    vec3 amt = vec3(5.0, 5.0, 5.0);
    vec3 q = opRepeatInfinite_2911168863(x__, amt);
    vec3 id = round((x__ / amt));
    float rx = sdBoxFrame((rotateX_4006468443((p_time + (id.x * 1.66))) * q), vec3(2.5, 2.5, 2.5), 0.2);
    float rz = sdBoxFrame((rotateZ_209996671((p_time + (id.z * 0.33))) * q), vec3(4.5, 2.5, 2.5), 0.2);
    float xz = opSmoothUnion(rx, rz, 0.75);
    float ry = sdBoxFrame((rotateY_3578297826((p_time + (id.y * 2.77))) * q), vec3(2.5, 2.5, 2.5), 0.2);
    float sdx = ((sin((((q.x * 3.0) + ((p_time + (0.5 * id.z)) * 2.0)) + (id.x * 5.66))) * 0.2) + 0.2);
    float sdy = ((sin((((q.y * 8.0) + (p_time * 4.0)) + (id.y * 0.066))) * 0.2) + 0.2);
    float sdz = ((sin((((q.z * 8.0) + ((p_time - 0.66) * 1.66)) + (id.z * 2.66))) * 0.2) + 0.2);
    float sd = ((sdx + sdy) + sdz);
    float xyz = ((opSmoothUnion(xz, ry, 0.75) * 0.9) + (sd * 0.1));
    float backdrop = sdSphere(x__, 1000.0);                    
    return min(xyz, (-backdrop));
}
#endif
#ifndef f_sceneNormal_3509136999
#define f_sceneNormal_3509136999
vec3 sceneNormal_3509136999(vec3 x__) { 
    vec3 dx = vec3(0.001, 0.0, 0.0);
    vec3 dy = vec3(0.0, 0.001, 0.0);
    vec3 dz = vec3(0.0, 0.0, 0.001);
    float dfdx = ((scene_1411025084((x__ + dx)) - scene_1411025084((x__ - dx))) / 0.002);
    float dfdy = ((scene_1411025084((x__ + dy)) - scene_1411025084((x__ - dy))) / 0.002);
    float dfdz = ((scene_1411025084((x__ + dz)) - scene_1411025084((x__ - dz))) / 0.002);                    
    return vec3(dfdx, dfdy, dfdz);
}
#endif

        
#ifndef STRUCT_MarchResult
#define STRUCT_MarchResult
struct MarchResult {

    vec3 position;
    bool hit;
    float travel;
    vec3 normal;
};
#endif
MarchResult marcher_4006392614(vec3 x__, vec3 y__) { 
    MarchResult result;
    bool False = false;
    bool True = true;
    result.hit = False;
    vec3 position = x__;
    int i;
    for (i = 0; i < 50; ++i) {
        float distance = scene_1411025084(position);
        if ((abs(distance) < 0.01)) {
            result.hit = True;
            result.position = position;
            break;
        }
        position = (position + ((y__ * distance) * 0.5));            
    }                    
    return result;
}
float ao_3401163839(vec3 x__, vec3 y__) { 
    float occ = 0.0;
    float sca = 1.0;
    int i = 0;
    for (i = 0; i < 5; ++i) {
        float h = (0.01 + (0.15 * (i / 4.0)));
        float d = scene_1411025084((x__ + (h * y__)));
        occ = (occ + ((h - d) * sca));
        sca = (sca * 0.95);            
    }                    
    return clamp((1.0 - (1.5 * occ)), 0.0, 1.0);
}

flat in int v_instance;
in vec3 v_boundsSize;

// -- fragmentConstants
int c_instance = v_instance;
int c_element = 0;
vec2 c_screenPosition = gl_FragCoord.xy / u_contentScale;
float c_contourPosition = 0.0;
vec3 c_boundsPosition = vec3(va_texCoord0, 0.0);
vec3 c_boundsSize = v_boundsSize;

void main(void) {
    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    {
        vec2 size = vec2(480.0, 480.0);
        float ar = (size.x / size.y);
        vec2 uv = vec2((va_texCoord0.x * ar), (1.0 - va_texCoord0.y));
        vec2 center = vec2(((240.0 / size.x) * ar), (240.0 / size.y));
        vec3 rayDir = normalize((p_viewMatrix * vec4((uv - center), -1.0, 0.0)).xyz);
        MarchResult result = marcher_4006392614((p_origin - vec3(NaN, NaN, NaN)), rayDir);
        vec3 temp_1; 
        if (result.hit) {
            vec3 normal = normalize(sceneNormal_3509136999(result.position));
            float aoed = ao_3401163839(result.position, normal);
            float d = ((dot(normal, vec3(0.2, 0.2, 1.0)) * 0.5) + (aoed * 0.3));
            temp_1 = vec3(d, d, d);
        } else { 
            temp_1 = vec3(0.0, 0.0, 0.0);
        }
        vec3 color = temp_1;
        x_fill = vec4(color.x, color.y, color.z, 1.0);
        
    }
    vec2 wd = fwidth(va_texCoord0 - vec2(0.5));
    vec2 d = abs((va_texCoord0 - vec2(0.5)) * 2.0);

    float irx = smoothstep(0.0, wd.x * 2.5, 1.0-d.x - vi_strokeWeight * 2.0 / vi_dimensions.x);
    float iry = smoothstep(0.0, wd.y * 2.5, 1.0-d.y - vi_strokeWeight * 2.0 / vi_dimensions.y);
    float ir = irx*iry;

    vec4 final = vec4(1.0);
    final.rgb = x_fill.rgb * x_fill.a;
    final.a = x_fill.a;

    float sa = (1.0-ir) * x_stroke.a;
    final.rgb = final.rgb * (1.0-sa) + x_stroke.rgb * sa;
    final.a = final.a * (1.0-sa) + sa;

       o_color = final;
}
// -------------
// shade-style-custom:rectangle-562194308
// created 2024-01-04T00:56:15.243456100
/*
0(203) : error C1503: undefined variable "NaN"
0(203) : error C1503: undefined variable "NaN"
0(203) : error C1503: undefined variable "NaN"
*/
