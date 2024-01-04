package lab

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

fun main() = application {
    configure {
        width = 770
        height = 578
    }
    program {
        // -- create the vertex buffer
        val geometry = vertexBuffer(vertexFormat {
            position(3)
        }, 4)

        // -- fill the vertex buffer with vertices for a unit quad
        geometry.put {
            write(Vector3(0.0, 0.0, 0.0))
            write(Vector3(0.0, 1.0, 0.0))
            write(Vector3(1.0, 0.0, 0.0))
            write(Vector3(1.0, 1.0, 0.0))
        }

        // -- create the secondary vertex buffer, which will hold transformations
        val transforms = vertexBuffer(vertexFormat {
            attribute("transform", VertexElementType.MATRIX44_FLOAT32)
        }, 385)

        // -- fill the transform buffer
        transforms.put {
            repeat(transforms.vertexCount) {
                write(transform {
                    translate(it * 1.0, 30.0)
                })
            }
        }
        extend {
            drawer.fill = ColorRGBa.WHITE
            drawer.shadeStyle = shadeStyle {
                vertexTransform = "x_viewMatrix = x_viewMatrix * i_transform;"
            }
            drawer.vertexBufferInstances(listOf(geometry), listOf(transforms), DrawPrimitive.TRIANGLE_STRIP, 1000)
        }
    }
}
