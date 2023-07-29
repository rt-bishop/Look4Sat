plugins {
    id("com.android.application") version "8.0.2" apply false
    id("com.android.library") version "7.1.2" apply false
    id("com.google.devtools.ksp") version "1.8.21-1.0.11" apply false
    id("org.jetbrains.kotlin.android") version "1.8.21" apply false
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}
