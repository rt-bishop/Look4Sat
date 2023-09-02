plugins {
    id("com.android.application") version "8.1.1" apply false
    id("com.android.library") version "8.1.1" apply false
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.layout.buildDirectory)
}
