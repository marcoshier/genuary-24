package lab

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.camera.ParametricOrbital
import org.openrndr.extra.fx.patterns.Checkers
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.timer.repeat
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.CatmullRomChain3
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import org.openrndr.shape.toPath3D
import kotlin.random.Random

fun main() = application {
    configure {
        width = 1280
        height = 720
    }

    program {


        val walk = (0..30).foldIndexed(mutableListOf(Vector3.ZERO)) { i, acc, new ->
            val n = Int.uniform(-10, 10).toDouble()
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
        val path = CatmullRomChain3(walk, loop = false).toPath3D()

        val controller = object : Animatable() {

            var currentIdx = 0
            var currentT = 0.0
            val stages = List(walk.size) {
                it.toDouble() / walk.size.toDouble()
            }

            fun run() {
                if (currentIdx == walk.indices.last) currentIdx = 0
                ::currentT.animate(stages[currentIdx], 1000, Easing.CubicInOut).completed.listen {
                    currentIdx++
                    run()
                }
            }

        }
        controller.run()

        val left = viewBox(drawer.bounds.scaledBy(0.5, 1.0, 0.0, 0.5)) {

            val sph = sphereMesh(radius = 0.3)

            val cam = Orbital()
            cam.camera.setView(Vector3.ZERO, Spherical(45.0, 45.0, 25.0), 50.0)

            extend(cam)
            extend {
                drawer.clear(ColorRGBa.GRAY.shade(0.1))

                drawer.stroke = ColorRGBa.WHITE
                drawer.path(path)


                drawer.fill = ColorRGBa.RED
                drawer.translate(path.position(controller.currentT))
                cam.camera.setView(path.position(controller.currentT), Spherical(45.0, 45.0, 25.0), 50.0)
                drawer.vertexBuffer(sph, DrawPrimitive.TRIANGLE_STRIP)

            }

    }
        val right = viewBox(drawer.bounds.scaledBy(0.5, 1.0, 0.0, 0.5)){

            val po = ParametricOrbital()

            extend(po)
            extend {



                po.center = path.position(controller.currentT)
                drawer.clear(ColorRGBa.BLUE.shade(0.1))

            }
        }

        extend {

            controller.updateAnimation()


            left.draw()
//            drawer.translate(width / 2.0, 0.0)
//            right.draw()

        }
    }
}