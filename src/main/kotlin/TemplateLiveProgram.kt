import org.openrndr.MouseCursorHideMode
import org.openrndr.application
import org.openrndr.extra.olive.OliveScriptHost
import org.openrndr.extra.olive.oliveProgram

/**
 *   XX - Title
 */

fun main() = application {
    configure {
        width = 1080
        height = 1080
        windowAlwaysOnTop = true
        cursorHideMode = MouseCursorHideMode.HIDE
    }
    oliveProgram(scriptHost = OliveScriptHost.JSR223) {

        extend {


        }
    }
}