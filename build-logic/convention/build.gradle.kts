import org.gradle.initialization.DependenciesAccessors
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    gradle.serviceOf<DependenciesAccessors>().classes.asFiles.forEach {
        compileOnly(files(it.absolutePath))
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

group = "com.rtbishop.look4sat.build_logic.convention"

gradlePlugin {
    plugins {
        register("applicationPlugin") {
            id = libs.plugins.convention.applicationPlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.ApplicationPlugin"
        }
        register("coreDataPlugin") {
            id = libs.plugins.convention.coreDataPlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.CoreDataPlugin"
        }
        register("coreDomainPlugin") {
            id = libs.plugins.convention.coreDomainPlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.CoreDomainPlugin"
        }
        register("corePresentationPlugin") {
            id = libs.plugins.convention.corePresentationPlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.CorePresentationPlugin"
        }
        register("featurePlugin") {
            id = libs.plugins.convention.featurePlugin.get().pluginId
            implementationClass = "com.rtbishop.look4sat.convention.FeaturePlugin"
        }
    }
}
