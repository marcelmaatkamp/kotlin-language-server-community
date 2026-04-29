package org.javacs.kt

import org.hamcrest.Matchers.*
import org.javacs.kt.classpath.*
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.BeforeClass
import java.nio.file.Files

class ClassPathTest {
    companion object {
        @JvmStatic @BeforeClass fun setupLogger() {
            LOG.connectStdioBackend()
        }
    }

    @Test fun `find gradle classpath`() {
        val workspaceRoot = testResourcesRoot().resolve("additionalWorkspace")
        val buildFile = workspaceRoot.resolve("build.gradle")

        assertTrue(Files.exists(buildFile))

        val resolvers = defaultClassPathResolver(listOf(workspaceRoot))
        print(resolvers)
        val classPath = resolvers.classpathOrEmpty.map { it.toString() }

        assertThat(classPath, hasItem(containsString("junit")))
    }

    @Test fun `find maven classpath`() {
        val workspaceRoot = testResourcesRoot().resolve("mavenWorkspace")
        val buildFile = workspaceRoot.resolve("pom.xml")

        assertTrue(Files.exists(buildFile))

        val resolvers = defaultClassPathResolver(listOf(workspaceRoot))
        print(resolvers)
        val classPath = resolvers.classpathOrEmpty.map { it.toString() }

        assertThat(classPath, hasItem(containsString("junit")))
    }

    @Test fun `find kotlin stdlib`() {
        assertThat(findKotlinStdlib(), notNullValue())
    }

    @Test fun `find Android generated Java source roots`() {
        val workspaceRoot = testResourcesRoot().resolve("androidGeneratedBuildConfig")
        val javaSourceRoots = findJavaSourceRoots(workspaceRoot, ScriptsConfiguration())
            .map { workspaceRoot.relativize(it).toString() }

        assertThat(javaSourceRoots, hasItem("app/build/generated/source/buildConfig/dev/debug"))
        assertThat(javaSourceRoots, not(hasItem("app/build/generated/source/buildConfig/prod/release")))
        assertThat(javaSourceRoots, not(hasItem(containsString("BuildConfig.java"))))
    }
}
