import kotlinx.coroutines.yield
import org.openrndr.MouseCursorHideMode
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.presets.DIM_GRAY
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shadergenerator.compute.computeTransform
import org.openrndr.extra.shadergenerator.dsl.Symbol
import org.openrndr.extra.shadergenerator.dsl.functions.function
import org.openrndr.extra.shadergenerator.dsl.functions.symbol
import org.openrndr.extra.shadergenerator.dsl.shadestyle.fragmentTransform
import org.openrndr.extra.shadergenerator.dsl.shadestyle.vertexTransform
import org.openrndr.extra.shadergenerator.dsl.structs.get
import org.openrndr.extra.shadergenerator.dsl.structs.getValue
import org.openrndr.extra.videoprofiles.h265
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.h264
import org.openrndr.launch
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.smoothstep
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 *   00 - 1PPP (1 particle per pixel)
 */

val size = 240

fun main() = application {
    configure {
        width = size
        height = size
        windowAlwaysOnTop = true
        cursorHideMode = MouseCursorHideMode.HIDE
    }
    program {

        val bs = ParticleBuffer()
        val sb = structuredBuffer(bs)

        var destination = drawer.bounds.center

        val computeInitialPositions = computeStyle {
            computeTransform {
                val b_buffer by parameter<ParticleBuffer>()
                val id by c_giid.x.int

                val getXY by function<Int, Vector2> {
                    val getX by it.div(width.symbol).double
                    val getY by floor(it.double.mod(width * 1.0))

                    Vector2(getX, getY)
                }

                val wang_hash by function<Double, Double> {
                    var p by variable<Double>()
                    p = it
                    p = fract(p * .1031);
                    p *= p + 33.33;
                    p *= p + p;
                    fract(p);
                }

                val x by getXY(id).x
                val y by getXY(id).y

                val h by hash11(id.double)

                val pos by Vector2(x, y)

                val min by simplex13(Vector3(pos.x * 0.005, pos.y * 0.008, (id.double))) * 0.5 + 0.5
                val max by (simplex13(Vector3(pos.x * 0.005, pos.y * 0.008, 1.0.symbol)) * 0.5 + 0.5)

                b_buffer.initialPositions[id] = pos
                b_buffer.velocities[id] = Vector2(
                  min,
                  clamp(min + max, 0.0.symbol, 1.0.symbol)
                )
                b_buffer.colors[id]  = ColorRGBa(x.div(width * 1.0), y.div(height * 1.0), 1.0.symbol).vector4
            }
        }
        computeInitialPositions.buffer("buffer", sb)
        computeInitialPositions.execute(size * size, 1, 1)


        val moveParticles = computeStyle {
            computeTransform {
                val b_buffer by parameter<ParticleBuffer>()
                val id by c_giid.x.int
                val p_time by parameter<Double>()
                val p_destination by parameter<Vector2>()

                val vel by b_buffer.velocities[id]
                val initialPos by b_buffer.initialPositions[id]
                val linearPos by initialPos.mix(p_destination, smoothstep(vel.x, vel.y, p_time))

                val pos by initialPos

                val rand by function<Int, Double> { seed_ ->
                    var seed by variable<UInt>()
                    seed = seed_.uint
                    seed = seed.xor(seed.shl(21.symbol.uint));
                    seed = seed.xor(seed.shr(35));
                    seed = seed.xor(seed.shl(4.symbol.uint));

                    seed * (1.0 / 4294967296.0)
                }

                val randX by rand(id) * 3.5

                val n by simplex33(Vector3(pos.x * 0.0005 + p_time + randX, pos.y * 0.0008 + p_time + randX, p_time * randX)).times(width.symbol.double * 0.05).xy
               // val p by snoise(pos * sin(pos.y) + n) * 400.0
                val perturbedPos by linearPos.plus(n * sin(p_time * PI))


                b_buffer.positions[id] = perturbedPos
            }
        }
        moveParticles.buffer("buffer", sb)
        moveParticles.parameter("destination", destination)
        moveParticles.parameter("time", mouse.position.x / width)
        moveParticles.execute(size * size, 1, 1)


        val ss = shadeStyle {
            vertexPreamble
            vertexTransform {
                val b_buffer by parameter<ParticleBuffer>()
                var color by varyingOut<Vector4>()

                val p by b_buffer.positions[c_instance]
                val c by b_buffer.colors[c_instance]

                x_position += Vector3(p.x, p.y, 0.0.symbol)
                color = c
            }
            fragmentTransform {
                val color by varyingIn<Vector4>()
                x_fill = Vector4(color.x, color.y, 1.0.symbol, 1.0.symbol)
            }
        }
        ss.buffer("buffer", sb)


        val pq = pixelQuad()

        val rt = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }


        fun tick() {
            val t = sin(seconds * 0.25 * PI) * 0.5 + 0.5

            if (t < 0.01) destination = drawer.bounds.uniform(4.0)

            moveParticles.parameter("time", smoothstep(0.1, 0.9, t) )
            moveParticles.parameter("destination", destination)
            moveParticles.execute(size * size, 1, 1)

            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa.DIM_GRAY.shade(0.2))
                drawer.shadeStyle = ss
                drawer.vertexBufferInstances(listOf(pq), emptyList(), DrawPrimitive.TRIANGLE_STRIP, size * size)
            }
        }



        val render = false

        val s = ScreenRecorder().apply {
            frameRate = 60
            maximumDuration = 30.0
            quitAfterMaximum = true
        }

        extend(s)

        extend {

            if(render) {
                if (frameCount > 30 * 70) application.exit()
            }

            tick()
            drawer.image(rt.colorBuffer(0))

        }



    }
}


fun pixelQuad(): VertexBuffer {
    val geometry = vertexBuffer(vertexFormat {
        position(3)
    }, 4)

    geometry.put {
        write(Vector3(0.0, 0.0, 0.0))
        write(Vector3(0.0, 1.0, 0.0))
        write(Vector3(1.0, 0.0, 0.0))
        write(Vector3(1.0, 1.0, 0.0))
    }

    return geometry
}

class ParticleBuffer: Struct<ParticleBuffer>() {
    val positions by arrayField<Vector2>(size * size)
    val initialPositions by arrayField<Vector2>(size * size)
    val velocity by arrayField<Vector2>(size * size)
    val colors by arrayField<Vector4>(size * size)
}

val Symbol<ParticleBuffer>.positions by ParticleBuffer::positions
val Symbol<ParticleBuffer>.initialPositions by ParticleBuffer::initialPositions
val Symbol<ParticleBuffer>.velocities by ParticleBuffer::velocity
val Symbol<ParticleBuffer>.colors by ParticleBuffer::colors