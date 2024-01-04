package lib


import org.openrndr.draw.*
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.shape.Rectangle
import kotlin.math.cos


val fx_fluid_distort2 = """
    // created by florian berger (flockaroo) - 2016
    // License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.


#define RotNum 5
//#define SUPPORT_EVEN_ROTNUM

const float ang = 2.0 * 3.1415926535 / float(RotNum);
mat2 m = mat2(cos(ang), sin(ang), -sin(ang), cos(ang));
mat2 mh = mat2(cos(ang*0.5), sin(ang*0.5), -sin(ang*0.5), cos(ang*0.5));

uniform sampler2D tex0;
uniform float time;
uniform float random;

in vec2 v_texCoord0;
uniform vec2 targetSize;

uniform float blend;

out vec4 o_color;

float getRot(vec2 pos, vec2 b) {
    vec2 Res = textureSize(tex0, 0) * 2.0;
    vec2 p = b;
    float rot = 0.0;
    for (int i = 0; i < RotNum; i++) {
        rot += dot(texture(tex0, fract((pos + p) / Res.xy)).xy -vec2(0.5), p.yx * vec2(1, -1));
        p = m * p;
    }
    return rot / float(RotNum)/dot(b, b);
}

void main() {
    vec2 pos = v_texCoord0 * targetSize;
    vec2 Res = textureSize(tex0, 0);

    vec2 b = vec2(cos(ang * random), sin(ang * random));
    vec2 v = vec2(0);
    float bbMax = 0.5 * Res.y;
    bbMax *= bbMax;
    for (int l = 0; l < 20; l++) {
        if (dot(b, b) > bbMax) break;
        vec2 p = b;
        for (int i = 0; i < RotNum; i++) {
            #ifdef SUPPORT_EVEN_ROTNUM
            v += p.yx * getRot(pos + p, -mh * b);
            #else
            // this is faster but works only for odd RotNum
            v += p.yx * getRot(pos + p, b);
            #endif
            p = m*p;
        }
        b *= 2.0;
    }
    o_color = vec4(0.0, 0.0, 0.0, 1.0);
    o_color.xy = texture(tex0, fract((pos + v * vec2(-1, 1) * 2.0) / Res.xy)).xy * (1.0-blend) + v_texCoord0 * blend;
}""".trimIndent()

private class UVMap: Filter( filterShaderFromCode(fx_uvmap, "uvmap"))

private class FluidDistortFilter2 : Filter(filterShaderFromCode(fx_fluid_distort2, "fluid-distort2")) {
    var blend : Double by parameters
    var random: Double by parameters
    init {
        blend = 0.0
        random = 0.0
    }
}

class FluidDistort2 : Filter1to1() {
    var blend: Double = 1.0

    var outputUV = false

    private val distort = FluidDistortFilter2()
    private val uvmap = UVMap()

    private var buffer0: ColorBuffer? = null
    private var buffer1: ColorBuffer? = null
    private var index = 0
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)
        distort.blend = blend
        distort.random = cos(index*0.5)*2.5+0.5

        buffer0?.let {
            if (!it.isEquivalentTo(target[0])) {
                it.destroy()
            }
        }
        if (buffer0 == null) {
            buffer0 = target[0].createEquivalent()
        }

        buffer1?.let {
            if (!it.isEquivalentTo(target[0])) {
                it.destroy()
            }
        }
        if (buffer1 == null) {
            buffer1 = target[0].createEquivalent()
        }
        val buffers = arrayOf(buffer0!!, buffer1!!)
        distort.apply(buffers[index%2], buffers[(index+1)%2], clip)

        if (!outputUV) {
            uvmap.apply(arrayOf(buffers[(index + 1) % 2], source[0]), target[0], clip)
        } else {
            buffers[(index+1)%2]. copyTo(target[0])
        }
        index++
        blend = 0.0
    }

}