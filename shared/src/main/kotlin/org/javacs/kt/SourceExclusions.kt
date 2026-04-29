package org.javacs.kt

import org.javacs.kt.util.filePath
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

// TODO: Read exclusions from gitignore/settings.json/... instead of
// hardcoding them
class SourceExclusions(
    private val workspaceRoots: Collection<Path>,
    private val scriptsConfig: ScriptsConfiguration
) {
    val excludedPatterns = (listOf(
        ".git", ".hg", ".svn",                                                      // Version control systems
        ".idea", ".idea_modules", ".vs", ".vscode", ".code-workspace", ".settings", // IDEs
        "bazel-*", "bin", "build", "node_modules", "target",                        // Build systems
    ) + when {
        !scriptsConfig.enabled -> listOf("*.kts")
        !scriptsConfig.buildScriptsEnabled -> listOf("*.gradle.kts")
        else -> emptyList()
    })

    private val exclusionMatchers = excludedPatterns
        .map { FileSystems.getDefault().getPathMatcher("glob:$it") }

    /** Finds all non-excluded files recursively. */
    fun walkIncluded(): Sequence<Path> = workspaceRoots.asSequence().flatMap { root ->
        root.toFile()
            .walk()
            .onEnter { isPathIncluded(it.toPath()) }
            .map { it.toPath() }
    }

    /** Tests whether the given URI is not excluded. */
    fun isURIIncluded(uri: URI) = uri.filePath?.let(this::isPathIncluded) ?: false

    /** Tests whether the given path is not excluded. */
    fun isPathIncluded(file: Path): Boolean = workspaceRoots.any { root ->
        file.startsWith(root) && (isGradleGeneratedSource(file, root) || isGradleGeneratedSourceParent(file, root) || isNotExcluded(file, root))
    }

    private fun isNotExcluded(file: Path, root: Path): Boolean = exclusionMatchers.none { matcher ->
        root
            .relativize(file)
            .any(matcher::matches)
    }

    private fun isGradleGeneratedSource(file: Path, root: Path): Boolean {
        val segments = root.relativize(file).map { it.toString() }
        return segments.windowed(2).any { it[0] == "build" && it[1] == "generated" }
    }

    private fun isGradleGeneratedSourceParent(file: Path, root: Path): Boolean {
        if (!file.toFile().isDirectory) return false

        val segments = root.relativize(file).map { it.toString() }
        return segments.withIndex().any { (index, segment) ->
            segment == "build" && (index == segments.lastIndex || segments.getOrNull(index + 1) == "generated")
        }
    }
}
