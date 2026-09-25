package ai.moechat.android.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ai.moechat.android.model.Quadrant
import ai.moechat.android.model.SubApp
import ai.moechat.android.model.Subject
import ai.moechat.android.model.SpacetimeAddress
import ai.moechat.android.ui.MoechatColors
import ai.moechat.android.web.HostContext
import ai.moechat.android.web.WebAppView
import java.util.Locale

/**
 * 宿主的骨架：顶栏（第 0 象限）＋ 主内容区（子应用）＋ 底栏（四个象限容器）。
 * 与 macOS 宿主的 RootView 一一对应。
 */
@Composable
fun RootScreen() {
    var subject by remember { mutableStateOf(Subject.Sample) }
    var address by remember { mutableStateOf(SpacetimeAddress.Now) }
    var expanded by remember { mutableStateOf<Quadrant?>(null) }
    var activeApp by remember { mutableStateOf<SubApp?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MoechatColors.Background)
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                subject = subject,
                address = address,
                onAddressChange = { address = it },
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                activeApp?.let { app ->
                    WebAppView(
                        app = app,
                        hostContext = HostContext(
                            subjectId = subject.id,
                            subjectName = subject.name,
                            spacetime = address.text,
                            theme = "dark",
                            // 子应用按 BCP-47 的兜底链解析；不支持的语言回落到 zh-Hans
                            locale = Locale.getDefault().toLanguageTag(),
                        ),
                        onMessage = { type, _ ->
                            if (type == "close") {
                                activeApp = null
                                expanded = null
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            BottomBar(
                expanded = expanded,
                onTap = { quadrant ->
                    // 点击容器：容器放大，同时默认应用自动进入。再点一次收起。
                    if (expanded == quadrant) {
                        expanded = null
                    } else {
                        expanded = quadrant
                        activeApp = quadrant.defaultApp
                    }
                },
                onAppTap = { app ->
                    activeApp = app
                    expanded = null
                },
            )
        }
    }
}
