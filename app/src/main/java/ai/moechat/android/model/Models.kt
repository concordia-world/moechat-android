package ai.moechat.android.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ai.moechat.android.R
import ai.moechat.android.ui.MoechatColors

/**
 * 子应用。它是 Web 应用，跑在宿主的 WebView 里，各自独立成仓库。
 *
 * `titleRes` 而不是 `title`：文案必须走资源，写死 String 就没法多语言，
 * 而且改起来不会漏——改了字段名，所有调用点都会报错。
 */
data class SubApp(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    /** 入口文件，相对于 assets/ 的路径。宿主据此拼出加载 URL，也据此判断「是否已安装」。 */
    val entry: String,
)

/**
 * 四象限。另有第 0 象限（时空），它不属于底部栏，由顶栏承担。
 * 四象限的默认应用：第一个是该象限的默认应用——点击容器直接进入它。
 */
enum class Quadrant(@StringRes val titleRes: Int, val accent: Color) {
    First(R.string.quadrant_first, MoechatColors.Q1),
    Second(R.string.quadrant_second, MoechatColors.Q2),
    Third(R.string.quadrant_third, MoechatColors.Q3),
    Fourth(R.string.quadrant_fourth, MoechatColors.Q4);

    val apps: List<SubApp>
        get() = when (this) {
            First -> listOf(SubApp("body", R.string.app_body, Icons.Filled.Person, entry("body")))
            Second -> listOf(
                SubApp("msglist", R.string.app_msglist, Icons.Filled.Message, entry("msglist")),
                SubApp("stash", R.string.app_stash, Icons.Filled.Bookmark, entry("stash")),
            )
            // 第三象限默认应用是「社会关系」，不是「人际」（需求方 2026-09-25 确认）
            Third -> listOf(
                SubApp("people", R.string.app_people, Icons.Filled.Groups, entry("people")),
            )
            Fourth -> listOf(
                SubApp("wallet", R.string.app_wallet, Icons.Filled.AccountBalanceWallet, entry("wallet")),
            )
        }

    val defaultApp: SubApp get() = apps.first()

    /** 展开后的网格形状由内容量决定：最多 3 列。 */
    val gridColumns: Int get() = apps.size.coerceIn(1, 3)
    val gridRows: Int get() = (apps.size + gridColumns - 1) / gridColumns

    private companion object {
        /** 子应用 id 与它在 assets 里的目录名必须一致，否则「装了却加载不到」。 */
        fun entry(id: String): String = "apps/$id/index.html"
    }
}

/** 主体 = 用户。产品支持登录与切换多个主体。 */
data class Subject(
    val id: String,
    val name: String,
    val initial: String,
) {
    companion object {
        val Sample = Subject(id = "1", name = "李鹏", initial = "李")
    }
}

/** 时空坐标。顶栏地址栏里显示并允许输入的就是它。 */
data class SpacetimeAddress(
    val year: Int,
    val month: Int,
    val day: Int,
    val longitude: Double,
    val latitude: Double,
) {
    val text: String get() = "$year&$month&$day,$longitude,$latitude"

    companion object {
        val Now = SpacetimeAddress(2026, 9, 25, 116.3974, 39.9093)
    }
}
