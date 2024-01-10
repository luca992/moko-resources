/*
 * Copyright 2024 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.gradle.generator.resources.string

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeSpec
import dev.icerock.gradle.generator.CodeConst
import dev.icerock.gradle.generator.PlatformResourceGenerator
import dev.icerock.gradle.generator.addAppleContainerBundleProperty
import dev.icerock.gradle.generator.localization.LanguageType
import dev.icerock.gradle.metadata.resource.StringMetadata
import org.apache.commons.text.StringEscapeUtils
import java.io.File

internal class AppleStringResourceGenerator(
    private val baseLocalizationRegion: String,
    private val resourcesGenerationDir: File
) : PlatformResourceGenerator<StringMetadata> {
    override fun imports(): List<ClassName> = emptyList()

    override fun generateInitializer(metadata: StringMetadata): CodeBlock {
        return CodeBlock.of(
            "StringResource(resourceId = %S, bundle = %L)",
            metadata.key,
            CodeConst.Apple.containerBundlePropertyName
        )
    }

    override fun generateResourceFiles(data: List<StringMetadata>) {
        data.processLanguages().forEach { (lang, strings) ->
            generateLanguageFile(
                language = LanguageType.fromLanguage(lang),
                strings = strings
            )
        }
    }

    override fun generateBeforeProperties(
        builder: TypeSpec.Builder,
        metadata: List<StringMetadata>
    ) {
        builder.addAppleContainerBundleProperty()
    }

    private fun generateLanguageFile(language: LanguageType, strings: Map<String, String>) {
        val resDir = File(resourcesGenerationDir, language.appleResourcesDir)
        val localizableFile = File(resDir, "Localizable.strings")
        resDir.mkdirs()

        val content = strings.mapValues { (_, value) ->
            convertXmlStringToAppleLocalization(value)
        }.map { (key, value) ->
            "\"$key\" = \"$value\";"
        }.joinToString("\n")
        localizableFile.writeText(content)

        if (language == LanguageType.Base) {
            val regionDir = File(resourcesGenerationDir, "$baseLocalizationRegion.lproj")
            regionDir.mkdirs()
            val regionFile = File(regionDir, "Localizable.strings")
            regionFile.writeText(content)
        }
    }

    // TODO should we do that?
    private fun convertXmlStringToAppleLocalization(input: String): String {
        return StringEscapeUtils.unescapeXml(input)
            .replace("\n", "\\n")
            .replace("\"", "\\\"")
    }
}
