import lib.FluidDistort2
import lib.fx_uvmap
import lib.oklabGLSL
import org.intellij.lang.annotations.Language
import org.openrndr.MouseCursorHideMode
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.drawImage
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.extra.shapes.toRounded
import org.openrndr.extra.timer.repeat
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Triangle
import kotlin.math.atan2
import kotlin.math.cos

/**
 *   01 - Perturbing palettes
 */



fun main() = application {
    configure {
        width = 1080
        height = 1080
        windowAlwaysOnTop = true
        cursorHideMode = MouseCursorHideMode.HIDE
    }
    program {

        val pf = PoissonFill()
        val spectrum = drawImage(width, height) {
            drawer.clear(ColorRGBa.TRANSPARENT)
            drawer.fill = ColorRGBa.WHITE
            drawer.shadeStyle = shadeStyle {
                fragmentPreamble = oklabGLSL
                fragmentTransform = """
                    vec2 uv = (c_boundsPosition.xy - 0.5) * 2.0;
        
                    float h = atan(uv.y, uv.x)/(2.0*M_PI);
                    float s = length(uv);
                    
                    
                    vec4 col = vec4(okhsl_to_srgb(vec3(h, s, 0.7)), 1.0);
                    col = s > 1.0 ? vec4(0.0) : col;
                    
                    x_fill = pow(col, vec4(1.4));
                """.trimIndent()

            }
            drawer.stroke = null
            drawer.rectangle(drawer.bounds)
        }
        pf.apply(spectrum, spectrum)

        val xyDistort = FluidDistort2().apply {
            blend = 1.0
        }


        val cb = spectrum.createEquivalent()


        val offset = 120.0

        class Picker(var pos: Vector2) {
            var color = ColorRGBa.TRANSPARENT

            private val triangles = listOf(
                Triangle(drawer.bounds.corner, drawer.bounds.corner + Vector2(width * 1.0, 0.0), drawer.bounds.center),
                Triangle(drawer.bounds.corner + drawer.bounds.dimensions, drawer.bounds.corner + Vector2(width * 1.0, 0.0), drawer.bounds.center),
                Triangle(drawer.bounds.corner + drawer.bounds.dimensions, drawer.bounds.corner + Vector2(0.0, height * 1.0), drawer.bounds.center),
                Triangle(drawer.bounds.corner, drawer.bounds.corner + Vector2(0.0, height * 1.0), drawer.bounds.center),
            )


            fun draw() {

                drawer.stroke = ColorRGBa.WHITE
                drawer.strokeWeight = 2.0

                drawer.fill = color

                val theta = Math.toDegrees(atan2(pos.y - drawer.bounds.center.y, pos.x - drawer.bounds.center.x))
                val polar = Polar(theta, pos.distanceTo(drawer.bounds.center) + 100.0).cartesian + drawer.bounds.center


                drawer.lineSegment(pos, polar)
                drawer.circle(pos, 5.0)

                drawer.circle(
                    Circle(polar,offset / 2.0)
                )


            }
        }


        var initialPositions = (0..6).map { Polar(Double.uniform(0.0, 360.0), 240.0).cartesian + drawer.bounds.center }
        var newPositions = initialPositions

        val pickers = initialPositions.map { Picker(it) }

        var oldT = System.currentTimeMillis()
        repeat(2.0) {
            oldT = System.currentTimeMillis()
            initialPositions = pickers.map { it.pos }
            newPositions = (0..6).map { Polar(Double.uniform(0.0, 360.0), 240.0).cartesian + drawer.bounds.center }
        }

        val s = ScreenRecorder().apply {
            frameRate = 60
            maximumDuration = 30.0
            quitAfterMaximum = true
        }

        extend(s)


        extend {

            drawer.image(spectrum)

            xyDistort.apply(spectrum, cb)
            drawer.image(cb)

            val positions = (initialPositions zip newPositions).mapIndexed { i, it ->
                val t = ((System.currentTimeMillis() - oldT) / 1000.0).coerceAtMost(1.0)
                it.first.mix(it.second,
                    smoothstep((1.0 / 7.0) * i * t, (1.0 / 7.0) * i + 0.1, t ))
            }

            val shad = cb.shadow.apply { download() }

            pickers.forEachIndexed { i, it ->
                it.pos = positions[i]

                val x = it.pos.x.toInt().coerceIn(0, cb.width)
                val y = it.pos.y.toInt().coerceIn(0, cb.height)

                it.color = shad[x, y]

                it.draw()
            }






        }
    }
}