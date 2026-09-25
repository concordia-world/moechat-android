import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * 把子应用的构建产物复制进 assets。
 *
 * 子应用各自独立成仓库，宿主只认「一个静态目录」。「安装」= 这一步。
 * 产物落在 `build/generated/` 而不是 `src/main/assets/`，所以仓库里不会出现子应用文件的副本，
 * 也就不需要额外 gitignore（`build/` 本来就忽略）。
 *
 * 子应用没构建时**直接失败并说清怎么办**，不静默产出空目录——
 * 空目录会让宿主加载出一个白屏，排查成本远高于一条报错。
 */
abstract class SyncSubAppsTask : DefaultTask() {
    /** 刻意标 @Internal：目录不存在时要走下面那条带说明的报错，而不是 Gradle 的输入校验错误。 */
    @get:Internal
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun sync() {
        val src = sourceDir.get().asFile
        if (!src.isDirectory) {
            throw GradleException(
                "子应用 msglist 未构建：${src.absolutePath} 不存在。\n" +
                    "先在 ${src.parentFile.absolutePath} 执行：npm install && npm run build",
            )
        }
        val dest = outputDir.get().asFile.resolve("apps/msglist")
        dest.deleteRecursively()
        src.copyRecursively(dest)
    }
}

val syncSubApps = tasks.register<SyncSubAppsTask>("syncSubApps") {
    // dist/ 变了就要重跑。增量判定交给 up-to-date 检查容易漏，
    // 这个任务只复制几百 KB，每次重跑的成本可以忽略。
    outputs.upToDateWhen { false }
    sourceDir.set(layout.projectDirectory.dir("../../msglist/dist"))
}

android {
    namespace = "ai.moechat.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.moechat.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(syncSubApps, SyncSubAppsTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.webkit)

    debugImplementation(libs.androidx.ui.tooling)
}
