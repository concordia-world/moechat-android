package ai.moechat.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * moechat 的统一视觉语言。
 * 五个原生宿主（macOS / Windows / Linux / iOS / Android）各自实现，但共用这一套取值——
 * 数值与 macOS 宿主的 Theme.swift 一一对应。
 */
object MoechatColors {
    val Background = Color(0xFF0A0B0D)

    val Surface = Color(0x0EFFFFFF)        // white 5.5%
    val SurfaceStroke = Color(0x1AFFFFFF)  // white 10%
    val Raised = Color(0x12FFFFFF)         // white 7%

    val PrimaryText = Color(0xEBFFFFFF)    // 92%
    val SecondaryText = Color(0xA8FFFFFF)  // 66%
    val TertiaryText = Color(0x6BFFFFFF)   // 42%
    val FaintText = Color(0x47FFFFFF)      // 28%

    val AddressBar = Color(0x0FFFFFFF)
    val AddressText = Color(0xCCFFFFFF)    // 80%
    val CellLabelText = Color(0xCCFFFFFF)  // 80%，展开态网格格子里的应用名
    val Cursor = Color(0xFF3E9BD6)

    // 四象限色
    val Q1 = Color(0xFFE8794A)
    val Q2 = Color(0xFF3E9BD6)
    val Q3 = Color(0xFFD65BA6)
    val Q4 = Color(0xFFD9A441)
}

/**
 * 宿主尺寸。取值与 macOS 宿主的 QuadrantContainerView 一一对应，
 * 规则见 `萌萌兔/moechat-宿主设计实现方案.md` 第三章、第四章。
 *
 * 收起态宽度不在这里：它由可用宽度等分得出（各端自适配），
 * 展开态宽度由子应用数量算出（各端必须一致）。
 */
object Metrics {
    const val shellPadding = 14
    const val barGap = 8
    const val barBottom = 26

    const val radiusContainer = 26f
    const val radiusContainerExpanded = 30f
    const val radiusGridCell = 18f

    /** 收起态高度固定，宽度由底栏等分。 */
    const val collapsedHeight = 108
    const val collapsedPadding = 11
    const val thumbCell = 26
    const val thumbGap = 5

    /** 展开态：格子、间距、内边距。 */
    const val cell = 68
    const val cellGap = 8
    const val expansionPadding = 16

    /** 展开宽 = 列数 × 格子 + (列数−1) × 间距 + 2 × 内边距。 */
    fun expandedWidth(columns: Int): Int =
        columns * cell + (columns - 1) * cellGap + expansionPadding * 2

    fun expandedHeight(rows: Int): Int =
        rows * cell + (rows - 1) * cellGap + expansionPadding * 2
}

@Composable
fun MoechatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = MoechatColors.Background,
            surface = MoechatColors.Background,
            onBackground = MoechatColors.PrimaryText,
            onSurface = MoechatColors.PrimaryText,
            primary = MoechatColors.Q2,
        ),
        content = content
    )
}
