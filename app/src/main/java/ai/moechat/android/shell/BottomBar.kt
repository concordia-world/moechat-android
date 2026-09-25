package ai.moechat.android.shell

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import ai.moechat.android.model.Quadrant
import ai.moechat.android.model.SubApp
import ai.moechat.android.ui.Metrics
import ai.moechat.android.ui.MoechatColors

/**
 * 底栏 —— 四个象限容器。
 * 点击容器：原地放大，露出子应用网格；两侧容器被挤压让位。
 * 再点一次收起。
 */
@Composable
fun BottomBar(
    expanded: Quadrant?,
    onTap: (Quadrant) -> Unit,
    onAppTap: (SubApp) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Metrics.shellPadding.dp)
            .padding(bottom = Metrics.barBottom.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Metrics.barGap.dp),
    ) {
        Quadrant.entries.forEach { quadrant ->
            val isExpanded = expanded == quadrant
            QuadrantContainer(
                quadrant = quadrant,
                isExpanded = isExpanded,
                onTap = { onTap(quadrant) },
                onAppTap = onAppTap,
                // 展开态宽度由子应用数量算出，不是固定 3 倍：内容少就窄。
                // 其余三个容器按权重平分剩下的宽度，被自然挤压。
                // 收起态四容器等分底栏宽度（各端屏幕宽不同，只能等分）。
                // 注意：等分在宽屏上会让收起宽超过展开宽，「点击放大」变成缩小。
                // 需求方 2026-09-25 决定暂不处理，见 moechat-宿主设计实现方案.md §4.2。
                modifier = if (isExpanded) {
                    Modifier.width(Metrics.expandedWidth(quadrant.gridColumns).dp)
                } else {
                    Modifier.weight(1f)
                },
            )
        }
    }
}

/**
 * 单个象限容器。
 * 收起态：显示「内容的缩略图」——里面有什么，图标就是什么（同 Android 桌面文件夹）。
 * 放大态：网格形状与缩略图一致，尺寸刚好容纳内容，不留空槽。
 * 放大尺寸不是固定 3 倍，由子应用数量算出——规则见 moechat-宿主设计实现方案.md 第四章。
 */
@Composable
private fun QuadrantContainer(
    quadrant: Quadrant,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onAppTap: (SubApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(
        (if (isExpanded) Metrics.radiusContainerExpanded else Metrics.radiusContainer).dp
    )
    val height = if (isExpanded) {
        Metrics.expandedHeight(quadrant.gridRows)
    } else {
        Metrics.collapsedHeight
    }

    Column(
        modifier = modifier
            .animateContentSize()
            .height(height.dp)
            .clip(shape)
            .background(if (isExpanded) quadrant.accent.copy(alpha = 0.15f) else MoechatColors.Surface)
            .border(
                width = 1.dp,
                color = if (isExpanded) quadrant.accent.copy(alpha = 0.55f) else MoechatColors.SurfaceStroke,
                shape = shape,
            )
            .clickable(onClick = onTap)
            .padding(
                if (isExpanded) Metrics.expansionPadding.dp else Metrics.collapsedPadding.dp
            ),
    ) {
        if (isExpanded) {
            ExpandedGrid(quadrant, onAppTap)
        } else {
            Thumbnail(quadrant)
            // 标签沉到底部，与 macOS 一致（靠 height(108) 撑开剩余空间）
            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Metrics.thumbGap.dp),
            ) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(quadrant.accent)
                )
                Text(
                    text = stringResource(quadrant.titleRes),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MoechatColors.SecondaryText,
                    maxLines = 1,
                    // 相邻容器展开时这里会被压窄。默认的 Clip 会把 "Possessions" 硬切成 "Poss"，
                    // 看起来像乱码；省略号至少表明「还有字」。
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 收起态：按与展开一致的布局缩小，所见即所得。 */
@Composable
private fun Thumbnail(quadrant: Quadrant) {
    val columns = quadrant.gridColumns
    val rows = quadrant.gridRows

    // 收起态宽度各端自适配（按可用宽度等分），缩略图格子必须跟着收窄，
    // 否则窄屏上子应用一多就会溢出容器。maxWidth 已被容器内边距扣过。
    BoxWithConstraints {
        val cell = min(
            Metrics.thumbCell.dp,
            (maxWidth - Metrics.thumbGap.dp * (columns - 1)) / columns,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Metrics.thumbGap.dp)) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Metrics.thumbGap.dp)) {
                    repeat(columns) { column ->
                        val index = row * columns + column
                        if (index < quadrant.apps.size) {
                            ThumbnailIcon(quadrant, quadrant.apps[index], cell)
                        } else {
                            Spacer(Modifier.size(cell))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailIcon(quadrant: Quadrant, app: SubApp, cell: Dp) {
    Box(
        modifier = Modifier
            .size(cell)
            .clip(RoundedCornerShape(cell * 0.28f))
            .background(quadrant.accent.copy(alpha = 0.34f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = app.icon,
            contentDescription = stringResource(app.titleRes),
            tint = MoechatColors.PrimaryText,
            modifier = Modifier.size(cell * 0.54f),
        )
    }
}

/** 放大态：不补空槽，网格就是内容本身。 */
@Composable
private fun ExpandedGrid(quadrant: Quadrant, onAppTap: (SubApp) -> Unit) {
    val columns = quadrant.gridColumns
    val rows = quadrant.gridRows

    Column(verticalArrangement = Arrangement.spacedBy(Metrics.cellGap.dp)) {
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Metrics.cellGap.dp)) {
                repeat(columns) { column ->
                    val index = row * columns + column
                    if (index < quadrant.apps.size) {
                        AppCell(quadrant, quadrant.apps[index], onAppTap)
                    } else {
                        Spacer(Modifier.size(Metrics.cell.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCell(quadrant: Quadrant, app: SubApp, onAppTap: (SubApp) -> Unit) {
    val shape = RoundedCornerShape(Metrics.radiusGridCell.dp)

    Column(
        modifier = Modifier
            .size(Metrics.cell.dp)
            .clip(shape)
            .background(quadrant.accent.copy(alpha = 0.20f))
            .border(1.dp, quadrant.accent.copy(alpha = 0.38f), shape)
            .clickable { onAppTap(app) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = app.icon,
            contentDescription = stringResource(app.titleRes),
            tint = MoechatColors.PrimaryText,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(app.titleRes),
            fontSize = 9.5.sp,
            color = MoechatColors.CellLabelText,
            maxLines = 1,
        )
    }
}
