package com.darmix.darmixpet.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object DarmixIcons {

    val Overlay: ImageVector by lazy {
        icon("Overlay") {
            outline {
                moveTo(8.5f, 13.5f); lineTo(6f, 13.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 3.5f, 11f)
                lineTo(3.5f, 6f)
                arcTo(2.5f, 2.5f, 0f, false, true, 6f, 3.5f)
                lineTo(13f, 3.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 15.5f, 6f)
                lineTo(15.5f, 9.5f)
            }
            wash { rrect(8.5f, 9.5f, 12f, 11f, 2.5f) }
            outline { rrect(8.5f, 9.5f, 12f, 11f, 2.5f) }
            outline { moveTo(8.5f, 13.5f); lineTo(20.5f, 13.5f) }
        }
    }

    val Hourglass: ImageVector by lazy {
        icon("Hourglass") {
            wash {
                moveTo(8.5f, 20.5f); lineTo(8.5f, 18f)
                curveTo(8.5f, 16.8f, 10.5f, 15.6f, 12f, 14.5f)
                curveTo(13.5f, 15.6f, 15.5f, 16.8f, 15.5f, 18f)
                lineTo(15.5f, 20.5f); close()
            }
            outline { moveTo(6.5f, 3.5f); lineTo(17.5f, 3.5f) }
            outline { moveTo(6.5f, 20.5f); lineTo(17.5f, 20.5f) }
            outline {
                moveTo(7.5f, 3.5f); lineTo(7.5f, 7f)
                curveTo(7.5f, 9f, 10f, 10.5f, 12f, 12f)
                curveTo(10f, 13.5f, 7.5f, 15f, 7.5f, 17f)
                lineTo(7.5f, 20.5f)
            }
            outline {
                moveTo(16.5f, 3.5f); lineTo(16.5f, 7f)
                curveTo(16.5f, 9f, 14f, 10.5f, 12f, 12f)
                curveTo(14f, 13.5f, 16.5f, 15f, 16.5f, 17f)
                lineTo(16.5f, 20.5f)
            }
        }
    }

    val Bell: ImageVector by lazy {
        icon("Bell") {
            val body: PathBuilder.() -> Unit = {
                moveTo(12f, 3.5f)
                curveTo(8.7f, 3.5f, 7f, 6f, 7f, 9f)
                lineTo(7f, 12.5f)
                curveTo(7f, 14.5f, 5.8f, 15.6f, 5f, 16.5f)
                lineTo(19f, 16.5f)
                curveTo(18.2f, 15.6f, 17f, 14.5f, 17f, 12.5f)
                lineTo(17f, 9f)
                curveTo(17f, 6f, 15.3f, 3.5f, 12f, 3.5f)
                close()
            }
            wash(body)
            outline(block = body)
            outline {
                moveTo(10f, 19.5f)
                curveTo(10.4f, 20.6f, 11.1f, 21f, 12f, 21f)
                curveTo(12.9f, 21f, 13.6f, 20.6f, 14f, 19.5f)
            }
        }
    }

    val Battery: ImageVector by lazy {
        icon("Battery") {
            wash { rrect(4.5f, 9f, 13f, 6f, 1.5f) }
            outline { rrect(2.5f, 7f, 17f, 10f, 3f) }
            outline { moveTo(21.5f, 10.5f); lineTo(21.5f, 13.5f) }
            outline {
                moveTo(13.5f, 9.2f); lineTo(10.8f, 12.6f)
                lineTo(13.4f, 12.6f); lineTo(11f, 15.6f)
            }
        }
    }

    val Eye: ImageVector by lazy {
        icon("Eye") {
            outline {
                moveTo(2.5f, 12f)
                curveTo(5f, 7.5f, 8.5f, 5.5f, 12f, 5.5f)
                curveTo(15.5f, 5.5f, 19f, 7.5f, 21.5f, 12f)
                curveTo(19f, 16.5f, 15.5f, 18.5f, 12f, 18.5f)
                curveTo(8.5f, 18.5f, 5f, 16.5f, 2.5f, 12f)
                close()
            }
            wash { circle(12f, 12f, 3.4f) }
            outline { circle(12f, 12f, 3.4f) }
        }
    }

    val Sun: ImageVector by lazy {
        icon("Sun") {
            wash { circle(12f, 12f, 4.2f) }
            outline { circle(12f, 12f, 4.2f) }
            outline {
                for (i in 0 until 8) {
                    val a = i * PI.toFloat() / 4f
                    moveTo(12f + 7.4f * cos(a), 12f + 7.4f * sin(a))
                    lineTo(12f + 9.6f * cos(a), 12f + 9.6f * sin(a))
                }
            }
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            outline(width = 2.8f) {
                moveTo(5.5f, 12.5f); lineTo(10f, 17f); lineTo(18.5f, 7.5f)
            }
        }
    }

    val Sparkle: ImageVector by lazy {
        icon("Sparkle") {
            val star: PathBuilder.() -> Unit = {
                moveTo(12f, 3f)
                curveTo(12.6f, 8f, 14f, 9.4f, 21f, 12f)
                curveTo(14f, 14.6f, 12.6f, 16f, 12f, 21f)
                curveTo(11.4f, 16f, 10f, 14.6f, 3f, 12f)
                curveTo(10f, 9.4f, 11.4f, 8f, 12f, 3f)
                close()
            }
            wash(block = star)
            outline(width = 1.6f, block = star)
        }
    }

    val Hat: ImageVector by lazy {
        icon("Hat") {
            val cone: PathBuilder.() -> Unit = {
                moveTo(12f, 3f)
                curveTo(11f, 7f, 9.4f, 11f, 7.3f, 16.2f)
                curveTo(9.6f, 17f, 14.4f, 17f, 16.7f, 16.2f)
                curveTo(15f, 12f, 14f, 8f, 12f, 3f)
                close()
            }
            wash(block = cone)
            outline(block = cone)
            outline { moveTo(8.3f, 13.4f); curveTo(10.5f, 14.3f, 13.5f, 14.3f, 15.7f, 13.4f) }
            outline { moveTo(3.2f, 17f); curveTo(6f, 19.8f, 18f, 19.8f, 20.8f, 17f) }
        }
    }

    val AppsGrid: ImageVector by lazy {
        icon("AppsGrid") {
            wash { rrect(4f, 4f, 7f, 7f, 2.2f) }
            wash { rrect(13f, 13f, 7f, 7f, 2.2f) }
            outline { rrect(4f, 4f, 7f, 7f, 2.2f) }
            outline { rrect(13f, 4f, 7f, 7f, 2.2f) }
            outline { rrect(4f, 13f, 7f, 7f, 2.2f) }
            outline { rrect(13f, 13f, 7f, 7f, 2.2f) }
        }
    }

    val Paw: ImageVector by lazy {
        icon("Paw") {
            val pad: PathBuilder.() -> Unit = {
                moveTo(12f, 13f)
                curveTo(9f, 13f, 6.8f, 15.5f, 6.8f, 17.6f)
                curveTo(6.8f, 19.5f, 8.4f, 20.3f, 10f, 19.9f)
                curveTo(11.2f, 19.6f, 12.8f, 19.6f, 14f, 19.9f)
                curveTo(15.6f, 20.3f, 17.2f, 19.5f, 17.2f, 17.6f)
                curveTo(17.2f, 15.5f, 15f, 13f, 12f, 13f)
                close()
            }
            wash(pad)
            outline(block = pad)
            val toes = listOf(
                Triple(5.6f, 11f, 1.9f), Triple(9.4f, 6.8f, 2f),
                Triple(14.6f, 6.8f, 2f), Triple(18.4f, 11f, 1.9f)
            )
            toes.forEach { (x, y, r) ->
                wash { circle(x, y, r) }
                outline { circle(x, y, r) }
            }
        }
    }

    val Cog: ImageVector by lazy {
        icon("Cog") {
            val gear: PathBuilder.() -> Unit = {
                for (i in 0 until 8) {
                    val c = i * (PI.toFloat() / 4f)
                    val pts = listOf(
                        (c - 0.30f) to 6.6f, (c - 0.20f) to 9.3f,
                        (c + 0.20f) to 9.3f, (c + 0.30f) to 6.6f
                    )
                    pts.forEachIndexed { j, (a, r) ->
                        val px = 12f + r * cos(a)
                        val py = 12f + r * sin(a)
                        if (i == 0 && j == 0) moveTo(px, py) else lineTo(px, py)
                    }
                }
                close()
            }
            wash(gear)
            outline(block = gear)
            outline { circle(12f, 12f, 3.2f) }
        }
    }

    val Clock: ImageVector by lazy {
        icon("Clock") {
            wash { circle(12f, 13f, 8f) }
            outline { circle(12f, 13f, 8f) }
            outline { moveTo(12f, 8.6f); lineTo(12f, 13f); lineTo(15f, 14.8f) }
            outline { moveTo(10f, 2.8f); lineTo(14f, 2.8f) }
            outline { moveTo(12f, 2.8f); lineTo(12f, 5f) }
        }
    }

    val Snowflake: ImageVector by lazy {
        icon("Snowflake") {
            wash { circle(12f, 12f, 2.4f) }
            outline {
                for (i in 0 until 3) {
                    val a = i * PI.toFloat() / 3f
                    moveTo(12f + 9.5f * cos(a), 12f + 9.5f * sin(a))
                    lineTo(12f - 9.5f * cos(a), 12f - 9.5f * sin(a))
                }
            }
            outline(width = 1.7f) {
                for (i in 0 until 6) {
                    val a = i * PI.toFloat() / 3f
                    val bx = 12f + 6.3f * cos(a)
                    val by = 12f + 6.3f * sin(a)
                    for (s in listOf(-1f, 1f)) {
                        val b = a + s * 0.78f
                        moveTo(bx, by)
                        lineTo(bx + 2.4f * cos(b), by + 2.4f * sin(b))
                    }
                }
            }
        }
    }

    val Ticket: ImageVector by lazy {
        icon("Ticket") {
            val ticket: PathBuilder.() -> Unit = {
                moveTo(4f, 6.5f); lineTo(20f, 6.5f); lineTo(20f, 10f)
                arcTo(2f, 2f, 0f, false, false, 20f, 14f)
                lineTo(20f, 17.5f); lineTo(4f, 17.5f); lineTo(4f, 14f)
                arcTo(2f, 2f, 0f, false, false, 4f, 10f)
                close()
            }
            wash(ticket)
            outline(block = ticket)
            outline(width = 1.7f) {
                moveTo(13.5f, 8.8f); lineTo(13.5f, 9.6f)
                moveTo(13.5f, 11.6f); lineTo(13.5f, 12.4f)
                moveTo(13.5f, 14.4f); lineTo(13.5f, 15.2f)
            }
        }
    }

    val Lock: ImageVector by lazy {
        icon("Lock") {
            wash { rrect(5.5f, 10.5f, 13f, 10f, 3f) }
            outline { rrect(5.5f, 10.5f, 13f, 10f, 3f) }
            outline {
                moveTo(8.5f, 10.5f); lineTo(8.5f, 8f)
                arcTo(3.5f, 3.5f, 0f, false, true, 15.5f, 8f)
                lineTo(15.5f, 10.5f)
            }
            outline { moveTo(12f, 15f); lineTo(12f, 17f) }
        }
    }

    val Search: ImageVector by lazy {
        icon("Search") {
            wash { circle(10.5f, 10.5f, 6f) }
            outline { circle(10.5f, 10.5f, 6f) }
            outline(width = 2.4f) { moveTo(15f, 15f); lineTo(20.5f, 20.5f) }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            outline(width = 2.4f) {
                moveTo(6f, 6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(6f, 18f)
            }
        }
    }

    val Plus: ImageVector by lazy {
        icon("Plus") {
            outline(width = 2.8f) {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        }
    }

    val Minus: ImageVector by lazy {
        icon("Minus") {
            outline(width = 2.8f) { moveTo(5f, 12f); lineTo(19f, 12f) }
        }
    }

    val ChevronLeft: ImageVector by lazy {
        icon("ChevronLeft") {
            outline(width = 2.8f) { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") {
            outline(width = 2.8f) { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
        }
    }

    val Moon: ImageVector by lazy {
        icon("Moon") {
            val moon: PathBuilder.() -> Unit = {
                moveTo(21f, 12.79f)
                arcTo(9f, 9f, 0f, true, true, 11.21f, 3f)
                arcTo(7f, 7f, 0f, false, false, 21f, 12.79f)
                close()
            }
            wash(moon)
            outline(block = moon)
        }
    }

    val HalfCircle: ImageVector by lazy {
        icon("HalfCircle") {
            wash {
                moveTo(12f, 3.5f)
                arcTo(8.5f, 8.5f, 0f, false, false, 12f, 20.5f)
                close()
            }
            outline { circle(12f, 12f, 8.5f) }
            outline { moveTo(12f, 3.5f); lineTo(12f, 20.5f) }
        }
    }

    val Heart: ImageVector by lazy {
        icon("Heart") {
            val heart: PathBuilder.() -> Unit = {
                moveTo(12f, 20f)
                curveTo(5f, 15f, 3f, 11f, 3f, 8.2f)
                curveTo(3f, 5.6f, 5f, 4f, 7.2f, 4f)
                curveTo(9.2f, 4f, 11f, 5.2f, 12f, 7f)
                curveTo(13f, 5.2f, 14.8f, 4f, 16.8f, 4f)
                curveTo(19f, 4f, 21f, 5.6f, 21f, 8.2f)
                curveTo(21f, 11f, 19f, 15f, 12f, 20f)
                close()
            }
            wash(heart)
            outline(block = heart)
        }
    }

    val Speaker: ImageVector by lazy {
        icon("Speaker") {
            val body: PathBuilder.() -> Unit = {
                moveTo(4f, 9.5f); lineTo(8f, 9.5f); lineTo(13f, 5.5f)
                lineTo(13f, 18.5f); lineTo(8f, 14.5f); lineTo(4f, 14.5f)
                close()
            }
            wash(body)
            outline(block = body)
            outline { moveTo(16f, 9.5f); quadTo(18.4f, 12f, 16f, 14.5f) }
            outline { moveTo(18.6f, 7f); quadTo(22.4f, 12f, 18.6f, 17f) }
        }
    }

    val Flashlight: ImageVector by lazy {
        icon("Flashlight") {
            val head: PathBuilder.() -> Unit = {
                moveTo(7f, 4f); lineTo(17f, 4f); lineTo(15.2f, 9f); lineTo(8.8f, 9f); close()
            }
            wash(head)
            outline(block = head)
            outline { rrect(9f, 9f, 6f, 12f, 2f) }
            outline { moveTo(12f, 12.5f); lineTo(12f, 15f) }
            outline(width = 1.7f) {
                moveTo(12f, 2.4f); lineTo(12f, 0.9f)
                moveTo(5.6f, 4.2f); lineTo(4.2f, 3f)
                moveTo(18.4f, 4.2f); lineTo(19.8f, 3f)
            }
        }
    }

    val Download: ImageVector by lazy {
        icon("Download") {
            outline(width = 2.4f) {
                moveTo(12f, 4f); lineTo(12f, 15f)
                moveTo(7.5f, 10.8f); lineTo(12f, 15.3f); lineTo(16.5f, 10.8f)
            }
            outline {
                moveTo(5f, 17.5f); lineTo(5f, 19.5f); lineTo(19f, 19.5f); lineTo(19f, 17.5f)
            }
        }
    }
}


private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()

private fun ImageVector.Builder.outline(
    width: Float = 1.9f,
    block: PathBuilder.() -> Unit
): ImageVector.Builder = path(
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = block
)

private fun ImageVector.Builder.wash(block: PathBuilder.() -> Unit): ImageVector.Builder = path(
    fill = SolidColor(Color.Black),
    fillAlpha = 0.28f,
    pathBuilder = block
)

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    close()
}

private fun PathBuilder.rrect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    moveTo(x + r, y)
    lineTo(x + w - r, y)
    arcTo(r, r, 0f, false, true, x + w, y + r)
    lineTo(x + w, y + h - r)
    arcTo(r, r, 0f, false, true, x + w - r, y + h)
    lineTo(x + r, y + h)
    arcTo(r, r, 0f, false, true, x, y + h - r)
    lineTo(x, y + r)
    arcTo(r, r, 0f, false, true, x + r, y)
    close()
}
