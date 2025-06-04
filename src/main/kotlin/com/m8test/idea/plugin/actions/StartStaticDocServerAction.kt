package com.m8test.idea.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import java.io.File


class StartStaticDocServerAction : AnAction("启动文档服务器") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val docsDir = File(System.getProperty("user.home"), ".m8test/docs")
        if (!docsDir.exists() || !docsDir.isDirectory) {
            Messages.showErrorDialog(project, "目录不存在: ${docsDir.absolutePath}", "错误")
            return
        }

        val folderOptions = docsDir.listFiles { file -> file.isDirectory }?.map { it.name } ?: emptyList()
        if (folderOptions.isEmpty()) {
            Messages.showErrorDialog(project, "没有可用的子文件夹", "错误")
            return
        }

        val folder = Messages.showEditableChooseDialog(
            "请选择要作为网站根目录的文件夹：",
            "选择根目录",
            null,
            folderOptions.toTypedArray(),
            folderOptions.first(),
            null
        ) ?: return

        val selectedDir = File(docsDir, folder)
        if (!selectedDir.exists() || !selectedDir.isDirectory) {
            Messages.showErrorDialog(project, "无效目录: ${selectedDir.absolutePath}", "错误")
            return
        }

        val portStr = Messages.showInputDialog(project, "请输入端口号（默认8080）", "端口输入", null)
        val port = portStr?.toIntOrNull() ?: 8080

        if (servers.containsKey(port)) {
            val existing = servers[port]!!
            val shouldRestart = Messages.showYesNoDialog(
                project,
                "端口 $port 的服务器已存在，根目录为：\n${existing.rootDir.absolutePath}\n\n是否重启为新目录：\n${selectedDir.absolutePath}？",
                "端口已被使用",
                null
            ) == Messages.YES
            if (!shouldRestart) return
            existing.server.stop()
        }

        val server = embeddedServer(Netty, port = port) {
            routing {
                staticFiles("/", selectedDir) {
                    default("index.html")
                }
            }
        }.start(wait = false)

        servers[port] = ServerInfo(port, selectedDir, server)
        Messages.showInfoMessage(
            project,
            "服务器已启动：http://localhost:$port\n根目录：${selectedDir.absolutePath}",
            "成功"
        )
    }

    /**
     * 服务器信息数据类，包含端口、根目录和服务实例
     */
    data class ServerInfo(
        val port: Int,
        val rootDir: File,
        val server: EmbeddedServer<*, *>
    )

    companion object {
        // 使用端口号作为 key 管理多个服务器及其信息
        private val servers: MutableMap<Int, ServerInfo> = mutableMapOf()
    }
}