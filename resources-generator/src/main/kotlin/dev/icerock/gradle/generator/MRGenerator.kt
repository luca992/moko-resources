/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.gradle.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.icerock.gradle.MRVisibility
import dev.icerock.gradle.metadata.GeneratedObject
import dev.icerock.gradle.metadata.GeneratedObjectModifier
import dev.icerock.gradle.metadata.GeneratorType
import dev.icerock.gradle.metadata.getActualInterfaces
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import java.io.File

abstract class MRGenerator(
    protected val settings: Settings,
    internal val generators: List<Generator>,
) {
    protected open val sourcesGenerationDir: File = settings.sourceSetDir.asFile
    protected open val resourcesGenerationDir: File = settings.resourcesDir.asFile
    protected open val assetsGenerationDir: File = settings.assetsDir.asFile


    internal fun generate() {
        sourcesGenerationDir.deleteRecursively()
        resourcesGenerationDir.deleteRecursively()
        assetsGenerationDir.deleteRecursively()

        beforeMRGeneration()

        val file = generateFileSpec()
        file?.writeTo(sourcesGenerationDir)

        afterMRGeneration()
    }

    abstract fun generateFileSpec(): FileSpec?

    // TODO not used. remove after complete migration of task configuration to Plugin configuration time
//    fun apply(project: Project): GenerateMultiplatformResourcesTask {
//        //TODO add sourceSetName
//        val name: String = project.displayName
//
//        val genTask = project.tasks.create(
//            "generateMR$name",
//            GenerateMultiplatformResourcesTask::class.java
//        ) {
//            it.inputs.property("mokoSettingsPackageName", settings.packageName)
//            it.inputs.property("mokoSettingsClassName", settings.className)
//            it.inputs.property("mokoSettingsVisibility", settings.visibility)
//            it.inputs.property(
//                "mokoSettingsIosLocalizationRegion",
//                settings.iosLocalizationRegion
//            )
//        }
//
//        apply(generationTask = genTask, project = project)
//
//        return genTask
//    }

    protected open fun beforeMRGeneration() = Unit
    protected open fun afterMRGeneration() = Unit

    protected abstract fun getMRClassModifiers(): Array<KModifier>

//    protected abstract fun apply(
//        generationTask: GenerateMultiplatformResourcesTask,
//        project: Project,
//    )

    protected open fun processMRClass(mrClass: TypeSpec.Builder) {}
    protected open fun getImports(): List<ClassName> = emptyList()

    interface Generator : ObjectBodyExtendable {
        val mrObjectName: String
        val resourceContainerClass: ClassName
            get() = ClassName("dev.icerock.moko.resources", "ResourceContainer")
        val resourceClassName: ClassName
        val inputFiles: Iterable<File>

        val type: GeneratorType

        fun generate(
            project: Project,
            inputMetadata: MutableList<GeneratedObject>,
            generatedObjects: MutableList<GeneratedObject>,
            targetObject: GeneratedObject,
            assetsGenerationDir: File,
            resourcesGenerationDir: File,
            objectBuilder: TypeSpec.Builder,
        ): TypeSpec?

        fun getImports(): List<ClassName>

        fun addActualOverrideModifier(
            propertyName: String,
            property: PropertySpec.Builder,
            inputMetadata: List<GeneratedObject>,
            targetObject: GeneratedObject,
        ): GeneratedObjectModifier {
            // Read actual interfaces of target object generator type
            val actualInterfaces: List<GeneratedObject> = inputMetadata.getActualInterfaces(
                generatorType = targetObject.generatorType
            )

            var containsInActualInterfaces = false

            // Search property in actual interfaces
            actualInterfaces.forEach { genInterface ->
                val hasInInterface = genInterface.properties.any {
                    it.name == propertyName
                }

                if (hasInInterface) {
                    containsInActualInterfaces = true
                }
            }

            return if (targetObject.isObject) {
                if (containsInActualInterfaces) {
                    property.addModifiers(KModifier.OVERRIDE)
                    GeneratedObjectModifier.Override
                } else {
                    when (targetObject.modifier) {
                        GeneratedObjectModifier.Actual -> {
                            property.addModifiers(KModifier.ACTUAL)
                            GeneratedObjectModifier.Actual
                        }
                        else -> {
                            GeneratedObjectModifier.None
                        }
                    }
                }
            } else {
                GeneratedObjectModifier.None
            }
        }
    }

    interface SourceSet {
        val name: String

        fun addSourceDir(directory: File)
        fun addResourcesDir(directory: File)
        fun addAssetsDir(directory: File)
    }

    data class Settings(
        val inputMetadataFiles: FileTree,
        val outputMetadataFile: File,
        val packageName: String,
        val className: String,
        val visibility: MRVisibility,
        val assetsDir: Directory,
        val sourceSetDir: Directory,
        val resourcesDir: Directory,
        val isStrictLineBreaks: Boolean,
        val ownResourcesFileTree: FileTree,
        val lowerResourcesFileTree: FileTree,
        val upperResourcesFileTree: FileTree,
        val iosLocalizationRegion: Provider<String>,
        val androidRClassPackage: Provider<String>,
    )
}
