package com.m8test.idea.plugin.template

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class M8TestModuleBuilder(private val language: String) : ModuleBuilder() {
    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val project = modifiableRootModel.project
        val root = createProjectStructure(project)
        modifiableRootModel.addContentEntry(root)
    }

    override fun getModuleType(): M8TestModuleType {
        return M8TestModuleTypeInstances.getInstance(language)!!
    }

    private fun createProjectStructure(project: Project): VirtualFile {
        val path = project.basePath ?: throw IllegalStateException("项目路径为空")
        val root = VfsUtil.createDirectories(path)
        copyTemplateDirectory(root)
        return root
    }

    private fun copyTemplateDirectory(root: VirtualFile) {
        val templateDir = moduleType.getTemplateDir()
        if (!templateDir.exists() || !templateDir.isDirectory) {
            throw IllegalArgumentException("模板目录不存在或不是目录: ${templateDir.canonicalPath}")
        }

        try {
            // 使用 Java NIO 复制目录
            Files.walk(templateDir.toPath()).forEach { sourcePath ->
                val relativePath = templateDir.toPath().relativize(sourcePath)
                val targetPath = Path.of(root.path).resolve(relativePath)
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath)
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
            // 刷新虚拟文件系统以识别新文件
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(root.path))?.refresh(false, true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
