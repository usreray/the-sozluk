package com.thesozluk.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/**
 * The app's mark: two parentheses that draw a drop. Same paths as the launcher icon
 * (res/drawable/ic_launcher_foreground.xml), cropped to the drop.
 */
object BrandIcons {
    private const val LEFT = "M50,22C40,38 30,50 30,64a22,22 0,0 0,20 21"
    private const val RIGHT = "M58,22c10,16 20,28 20,42a22,22 0,0 1,-20 21"

    private fun drop(name: String, left: Color, right: Color): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, viewportWidth = 72f, viewportHeight = 72f).apply {
            group(translationX = -18f, translationY = -17.5f) {
                addPath(addPathNodes(LEFT), stroke = SolidColor(left), strokeLineWidth = 8f, strokeLineCap = StrokeCap.Round)
                addPath(addPathNodes(RIGHT), stroke = SolidColor(right), strokeLineWidth = 8f, strokeLineCap = StrokeCap.Round)
            }
        }.build()

    /** The logo with its two parentheses in the given colors, for use with Image */
    fun logo(left: Color, right: Color): ImageVector = drop("ParenDropLogo", left, right)
}

/**
 * The favorite drop: Material's water drop without the shine line in its lower left, outlined
 * and filled.
 */
object FavoriteIcons {
    private const val OUTER = "M12,2C6.67,6.55 4,10.48 4,13.8C4,18.78 7.8,22 12,22S20,18.78 20,13.8C20,10.48 17.33,6.55 12,2Z"
    private const val INNER = "M12,20C8.65,20 6,17.43 6,13.8C6,11.46 7.95,8.36 12,4.66C16.05,8.36 18,11.45 18,13.8C18,17.43 15.35,20 12,20Z"

    val Drop: ImageVector by lazy {
        ImageVector.Builder("FavDrop", 24.dp, 24.dp, 24f, 24f).apply {
            addPath(addPathNodes(OUTER + INNER), pathFillType = PathFillType.EvenOdd, fill = SolidColor(Color.Black))
        }.build()
    }

    val DropFilled: ImageVector by lazy {
        ImageVector.Builder("FavDropFilled", 24.dp, 24.dp, 24f, 24f).apply {
            addPath(addPathNodes(OUTER), fill = SolidColor(Color.Black))
        }.build()
    }
}
