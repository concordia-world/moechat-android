package ai.moechat.android.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.moechat.android.model.SpacetimeAddress
import ai.moechat.android.model.Subject
import ai.moechat.android.ui.Metrics
import ai.moechat.android.ui.MoechatColors

/**
 * 顶栏 = 第 0 象限（时空）。
 * 左：主体图标，点击切换主体；中：可输入的地址栏；右：后退 / 前进 / 刷新。
 */
@Composable
fun TopBar(
    subject: Subject,
    address: SpacetimeAddress,
    onAddressChange: (SpacetimeAddress) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Metrics.shellPadding.dp)
            .padding(top = 34.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SubjectBadge(subject)

        AddressField(
            address = address,
            onAddressChange = onAddressChange,
            modifier = Modifier.weight(1f),
        )

        NavigationButtons()
    }
}

/** 主体图标 —— 时空环里嵌着当前主体，所以「点击它切换主体」是自然的。 */
@Composable
private fun SubjectBadge(subject: Subject) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, MoechatColors.SurfaceStroke, CircleShape)
            .background(MoechatColors.Surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MoechatColors.Q2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = subject.initial,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MoechatColors.Background,
            )
        }
    }
}

/** 地址栏 —— 装的是时空坐标，允许输入以跳转到别的时空。 */
@Composable
private fun AddressField(
    address: SpacetimeAddress,
    onAddressChange: (SpacetimeAddress) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(address) { mutableStateOf(address.text) }
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(MoechatColors.AddressBar)
            .border(1.dp, MoechatColors.SurfaceStroke, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MoechatColors.AddressText,
            ),
            cursorBrush = SolidColor(MoechatColors.Cursor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                // TODO: 解析输入并跳转。解析规则见待确认清单第 13 条。
                focusManager.clearFocus()
            }),
            modifier = Modifier.weight(1f),
        )

        // 未聚焦时露出一个静止光标，暗示这里可输入。
        Box(
            Modifier
                .size(width = 1.dp, height = 13.dp)
                .background(MoechatColors.Cursor)
        )
    }
}

@Composable
private fun NavigationButtons() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NavButton(Icons.Filled.ArrowBack)
        NavButton(Icons.Filled.ArrowForward)
        NavButton(Icons.Filled.Refresh)
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MoechatColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MoechatColors.SecondaryText,
            modifier = Modifier.size(15.dp),
        )
    }
}
