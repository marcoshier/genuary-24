import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.camera.ParametricOrbital
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.color.Invert
import org.openrndr.extra.fx.edges.CannyEdgeDetector
import org.openrndr.extra.fx.edges.Contour
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.Once
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.shadergenerator.dsl.functions.Matrix33Functions
import org.openrndr.extra.shadergenerator.dsl.functions.Matrix44Functions
import org.openrndr.extra.shadergenerator.dsl.functions.function
import org.openrndr.extra.shadergenerator.dsl.functions.symbol
import org.openrndr.extra.shadergenerator.dsl.shadestyle.fragmentTransform
import org.openrndr.extra.shadergenerator.phrases.dsl.functions.gradient
import org.openrndr.extra.shadergenerator.phrases.sdf.*
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.*
import org.openrndr.math.transforms.normalMatrix
import org.openrndr.shape.Path3D
import org.openrndr.shape.toPath3D
import kotlin.math.sin
import kotlin.random.Random

fun main() = application {
    configure {
        width = 540
        height = 540
        windowAlwaysOnTop = true
    }
    program {


        val s = ScreenRecorder().apply {
            frameRate = 60
            maximumDuration = 20.0
            quitAfterMaximum = true
        }

        extend(s)

        val o = ParametricOrbital()
        extend(o)


        val walk = (0..30).foldIndexed(mutableListOf(Vector3.ZERO)) { i, acc, new ->
            val n = Int.uniform(-10, 10).toDouble() * 5.0
            val r = Int.uniform(1, 4, Random(i))
            val v = when(r) {
                1 -> Vector3(acc.last().x +n, acc.last().y, acc.last().z)
                2 -> Vector3(acc.last().x, acc.last().y + n, acc.last().z)
                3 -> Vector3(acc.last().x, acc.last().y, acc.last().z + n)
                else -> error("whaat $r")
            }

            acc.add(v)
            acc
        }
        val path = Path3D.fromPoints(walk, false)

        val controller = object : Animatable() {

            var currentIdx = 0
            var currentT = 0.0
            val stages = List(walk.size) {
                it.toDouble() / walk.size.toDouble()
            }

            fun run() {
                if (currentIdx == walk.indices.last) currentIdx = 0
                ::currentT.animate(stages[currentIdx], 750, Easing.QuartInOut, 500).completed.listen {
                    currentIdx++
                    run()
                }
            }

        }
        controller.run()

        val ss = shadeStyle {
            fragmentTransform {
                val p_time by parameter<Double>()
                val p_origin by parameter<Vector3>()
                val va_texCoord0 by parameter<Vector2>()
                val p_viewMatrix by parameter<Matrix44>()


                val size by drawer.bounds.dimensions
                val ar by size.x / size.y
                val uv by Vector2(va_texCoord0.x * ar, 1.0 - va_texCoord0.y)

                val center by Vector2(drawer.bounds.center.x / size.x * ar, drawer.bounds.center.y / size.y)

                val rayDir by (p_viewMatrix * Vector4(uv - center, -1.0, 0.0)).xyz.normalized

                val opRepeatInfinite by function<Vector3, Vector3, Vector3> { p, s ->

                    val rX by p.x - round(p.x / s.x) * s.x
                    val rY by p.y - round(p.y / s.y) * s.y
                    val rZ by p.z - round(p.z / s.z) * s.z

                    Vector3(rX, rY, rZ)
                }



                val rotateX by function<Double, Matrix33> {
                    Matrix33.fromColumnVectors(
                        Vector3(1.0.symbol, 0.0.symbol, 0.0.symbol),
                        Vector3(0.0.symbol, cos(it), -sin(it)),
                        Vector3(0.0.symbol, sin(it), cos(it))
                    )
                }

                val rotateY by function<Double, Matrix33> {
                    Matrix33.fromColumnVectors(
                        Vector3(cos(it), 0.0.symbol, -sin(it)),
                        Vector3(0.0.symbol, 1.0.symbol, 0.0.symbol),
                        Vector3(sin(it), 0.0.symbol, cos(it))
                    )
                }

                val rotateZ by function<Double, Matrix33> {
                    Matrix33.fromColumnVectors(
                        Vector3(cos(it), -sin(it), 0.0.symbol),
                        Vector3(sin(it), cos(it), 0.0.symbol),
                        Vector3(0.0.symbol, 0.0.symbol, 1.0.symbol)
                    )
                }

                val scene by function<Vector3, Double> {

                    val amt by Vector3(5.0)
                    val q by opRepeatInfinite((it), amt)

                    val id by round(it/amt)

                    val rx by sdBoxFrame(rotateX(p_time + id.x * 1.66).times(q), Vector3(2.5, 2.5, 2.5).symbol, 0.2.symbol)
                    val rz by sdBoxFrame(rotateZ(p_time + id.z * 0.33).times(q), Vector3(4.5, 2.5, 2.5).symbol, 0.2.symbol)

                    val xz by opSmoothUnion(rx, rz, 0.75.symbol)

                    val ry by sdBoxFrame(rotateY(p_time + id.y * 2.77).times(q), Vector3(2.5, 2.5, 2.5).symbol, 0.2.symbol)


                    val sdx by sin(q.x * 3.0 + (p_time + 0.5 * id.z) * 2.0 + id.x * 5.66) * 0.2 + 0.2
                    val sdy by sin(q.y * 8.0 + p_time * 4.0 + id.y * 0.066) * 0.2 + 0.2
                    val sdz by sin(q.z * 8.0 + (p_time - 0.66) * 1.66 + id.z * 2.66) * 0.2 + 0.2

                    val sd by sdx + sdy + sdz

                    val xyz by opSmoothUnion(xz, ry, 0.75.symbol) * 0.9 + sd * 0.1

                    val backdrop by sdSphere(it, 1000.0.symbol)
                    min(xyz, -backdrop)

                }

                val sceneNormal by gradient(scene, 1E-3)
                val marcher by march(scene, 50)
                val ao by calcAO(scene)
                val result by marcher(p_origin, rayDir) //p_origin - Vector3(0.0.symbol, 0.0.symbol, p_time * 2.0)

                val color by Vector3(0.0.symbol, 0.0.symbol, 0.0.symbol).elseIf(result.hit) {
                    val normal by sceneNormal(result.position).normalized
                    val aoed by ao(result.position, normal)
                    val d by normal.dot(Vector3(0.2, 1.0, 1.0)) * 0.5 + aoed * 0.4

                    Vector3(d,d,d)
                }

                x_fill = Vector4(color.x, color.y, color.z, 1.0)

            }
        }

        extend(Post()) {
            val fb = FrameBlur()
            fb.blend = 0.45

            val ce = Contour()
            val i = Invert()
            ce.backgroundOpacity = 1.0

            ce.levels = 10.0
            ce.contourColor = ColorRGBa.WHITE
            ce.contourOpacity = 1.0
            ce.window = 2


            post { input, output ->
                val i0 = intermediate[0]
                val i1 = intermediate[0]
                ce.apply(input, i0)
                i.apply(i0, i1)
                fb.apply(i1, output)
            }


        }

        extend {


            controller.updateAnimation()

            drawer.clear(ColorRGBa.BLACK)

            o.center = path.position(controller.currentT)
            // o.phi = mod( sin(seconds) * 45.0 + 45.0, 180.0)
            //  o.theta = mod( 180.0 + seconds * 45.0, 360.0) - 180.0


            ss.parameter("viewMatrix", normalMatrix(o.camera.viewMatrix().inversed))
            ss.parameter("origin", (o.camera.viewMatrix().inversed * Vector4.UNIT_W).xyz)
            ss.parameter("time", seconds)



            drawer.defaults()
            drawer.shadeStyle = ss
            drawer.rectangle(drawer.bounds)

        }
    }
}