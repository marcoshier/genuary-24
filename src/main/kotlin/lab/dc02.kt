import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.shadergenerator.compute.computeTransform
import org.openrndr.extra.shadergenerator.dsl.Symbol
import org.openrndr.extra.shadergenerator.dsl.functions.symbol
import org.openrndr.extra.shadergenerator.dsl.shadestyle.fragmentTransform
import org.openrndr.extra.shadergenerator.dsl.shadestyle.vertexTransform
import org.openrndr.extra.shadergenerator.dsl.structs.get
import org.openrndr.extra.shadergenerator.dsl.structs.getValue
import org.openrndr.extra.shadergenerator.dsl.structs.setValue


class DoubleVal : Struct<DoubleVal>()  {
    var float by field<Double>()
}

var Symbol<DoubleVal>.float by DoubleVal::float

class BufferStruct : Struct<BufferStruct>() {
    val floats by arrayField<DoubleVal>(639)
}

val Symbol<BufferStruct>.floats by BufferStruct::floats

/**
 * A compute demo in which a compute style is used to generate positions in
 * a buffer structured by [BufferStruct]. A shade style is used to retrieve the
 * positions in the vertex transform.
 */
fun main() {
    application {
        program {
            val bufferStruct = BufferStruct()
            val buffer = structuredBuffer(bufferStruct)

            val cs = computeStyle {
                computeTransform {
                    val b_buffer by parameter<BufferStruct>()
                    // val time by parameter<Double>()
                    val p_time by parameter<Double>()


                    b_buffer.floats[c_giid.x.int].float = c_giid.x.double + sin(p_time) * 4.0
                }
            }
            cs.parameter("time", seconds)
            cs.buffer("buffer", buffer)
            cs.execute(640, 1, 1)

            val ss = shadeStyle {
                vertexTransform {
                    val b_buffer by parameter<BufferStruct>()
                    val x by b_buffer.floats[c_instance].float * 1.5
                    x_position += Vector3(0.0.symbol, x, 0.0.symbol)
                }
                fragmentTransform {
                    x_fill = Vector4(v_viewNormal.z, v_viewNormal.z, v_viewNormal.z, 1.0.symbol)
                }
            }
            ss.buffer("buffer", buffer)

            val sphere = sphereMesh()
            extend(Orbital())
            extend {
                cs.parameter("time", seconds)
                cs.execute(640, 1, 1)
                drawer.isolated {
                    drawer.shadeStyle = ss
                    drawer.vertexBufferInstances(listOf(sphere), emptyList(), DrawPrimitive.TRIANGLES, 140)
                }
            }
        }
    }
}