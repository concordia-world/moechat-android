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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * **主体容器** —— 装主体四个象限的那个半透明长条，四个象限容器在里面。
 *
 * 名字的由来：第 1-2-3-4 象限本来就是**主体的**象限。
 *
 * 点击容器：原地放大，露出子应用网格；其余容器让位。再点一次收起。
 * 结构与高度照系统底栏来（华为 Mate X5 的底栏就是一个半透明长圆角容器装着若干方形
 * 图标）。各端取各自系统的值，不强行相等。
 *
 * **宽度不撑满父级**：收起时贴着四个容器居中；展开时才撑满可用宽 ——
 * 那时需要空间放网格，也顺势把其余容器挤开。
 */
@Composable
fun BottomBar(
    expanded: Quadrant?,
    onTap: (Quadrant) -> Unit,
    onAppTap: (SubApp) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 外边距在主体容器的背景**之外**，所以这两层 padding 落在外层 Box 上
            .padding(horizontal = Metrics.shellPadding.dp)
            .padding(bottom = Metrics.barBottom.dp),
        contentAlignment = Alignment.Center,
    ) {
    Row(
        modifier = Modifier
            // 收起：不给宽度约束，Row 自己贴着内容 —— 这是「不必撑满父级」的落点
            // 展开：撑满可用宽，展开的容器变宽、其余被挤压
            .then(if (expanded == null) Modifier else Modifier.fillMaxWidth())
            .clip(RoundedCornerShape(Metrics.radiusBar.dp))
            .background(MoechatColors.BarSurface)
            .padding(Metrics.barPadding.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded == null) {
            // 收起态用固定间距，主体容器的宽度才由内容决定
            Arrangement.spacedBy(Metrics.containerGap.dp)
        } else {
            // 展开态间距要能自适应：格子宽了，均分才能把其余容器挤开
            Arrangement.SpaceEvenly
        },
    ) {
        Quadrant.entries.forEach { quadrant ->
            val isExpanded = expanded == quadrant
            QuadrantContainer(
                quadrant = quadrant,
                isExpanded = isExpanded,
                onTap = { onTap(quadrant) },
                onAppTap = onAppTap,
                // 收起态是**方形**，边长对齐系统底栏的图标；
                // 展开态按内容变宽，其余容器被均分重新排开。
                modifier = if (isExpanded) {
                    Modifier.width(Metrics.expandedWidth(quadrant.gridColumns).dp)
                } else {
                    Modifier.size(Metrics.collapsedHeight.dp)
                },
            )
        }
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
        // 收起态是「缩略图在上、标签在下」的竖排，两者要对齐在同一条竖中线上。
        // 展开态是网格，左对齐，居中会把它推到中间 —— 所以这里只影响收起态：
        // Spacer(weight) 在展开态不存在，ExpandedGrid 自带对齐。
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isExpanded) {
            ExpandedGrid(quadrant, onAppTap)
        } else {
            Thumbnail(quadrant)
            // 标签沉到底部、**居中**。
            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(quadrant.titleRes),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                // 用象限色。原先文字左边有个同色小圆点，文字居中之后圆点会把居中破坏掉，
                // 所以去掉圆点、让文字本身承担这个色彩线索。
                color = quadrant.accent,
                maxLines = 1,
                // 相邻容器展开时这里会被压窄。默认的 Clip 会把 "Possessions" 硬切成 "Poss"，
                // 看起来像乱码；省略号至少表明「还有字」。
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
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
    BoxWithConstraints(contentAlignment = Alignment.Center) {
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
