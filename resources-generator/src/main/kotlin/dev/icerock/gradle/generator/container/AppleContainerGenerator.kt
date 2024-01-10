/*
 * Copyright 2024 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.gradle.generator.container

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import dev.icerock.gradle.generator.CodeConst
import dev.icerock.gradle.generator.PlatformContainerGenerator
import dev.icerock.gradle.generator.addAppleResourcesBundleProperty

internal class AppleContainerGenerator(
    private val bundleIdentifier: String
) : PlatformContainerGenerator {
    override fun getImports(): List<ClassName> {
        return listOf(
            CodeConst.Apple.nsBundleName,
            CodeConst.Apple.loadableBundleName
        )
    }

    override fun generateBeforeTypes(builder: TypeSpec.Builder) {
        builder.addAppleResourcesBundleProperty(bundleIdentifier)
    }
}
