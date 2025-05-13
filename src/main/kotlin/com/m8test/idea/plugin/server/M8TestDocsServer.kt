package com.m8test.idea.plugin.server

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.m8test.gradle.util.PathUtils
import com.m8test.idea.plugin.util.LogUtils
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import java.io.File
import java.net.ServerSocket
import java.nio.file.Files

abstract class M8TestDocsServer(path: String) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val port: Int
    private val staticDir: File

    fun getUrl(project: Project): String {
        if (server == null) {
            startServer(project)
        }
        return "http://localhost:$port"
    }

    init {
        // 初始化端口和静态目录
        port = getAvailablePort()
        staticDir = initializeStaticDir(File(PathUtils.getM8TestPath("docs"), path))
        servers.add(this)
    }

    /**
     * 初始化静态文件目录，确保目录存在且可写。
     */
    private fun initializeStaticDir(dir: File): File {
        try {
            if (!dir.exists()) {
                Files.createDirectories(dir.toPath())
                LogUtils.info("Created static directory: ${dir.absolutePath}")
            } else if (!dir.isDirectory) {
                LogUtils.error("Static path is not a directory: ${dir.absolutePath}")
                throw IllegalStateException("Static path is not a directory: ${dir.absolutePath}")
            }
            if (!dir.canWrite()) {
                LogUtils.error("Static directory is not writable: ${dir.absolutePath}")
                throw IllegalStateException("Static directory is not writable: ${dir.absolutePath}")
            }
            return dir
        } catch (e: Exception) {
            LogUtils.error("Failed to initialize static directory: ${dir.absolutePath}", e)
            throw IllegalStateException("Failed to initialize static directory", e)
        }
    }

    /**
     * 获取可用端口。
     */
    private fun getAvailablePort(): Int {
        try {
            ServerSocket(0).use { socket ->
                val selectedPort = socket.localPort
                LogUtils.info("Selected available port: $selectedPort")
                return selectedPort
            }
        } catch (e: Exception) {
            LogUtils.error("Failed to get available port", e)
            throw IllegalStateException("Failed to get available port", e)
        }
    }

    /**
     * 启动 HTTP 静态服务器并注册项目关闭监听。
     */
    private fun startServer(project: Project) {
        try {
            LogUtils.info("Starting static server at http://localhost:$port, serving directory: ${staticDir.absolutePath}")
            server = embeddedServer(Netty, port = port) {
                routing {
                    staticFiles("/", staticDir, "index.html")
                }
            }.start(wait = false)
            LogUtils.info("Static server started successfully at http://localhost:$port")

            // 注册项目关闭监听
            ProjectManager.getInstance().addProjectManagerListener(project, object : ProjectManagerListener {
                override fun projectClosed(project: Project) {
                    LogUtils.info("Project ${project.name} closed, stopping server")
                    stopServer()
                }
            })
        } catch (e: Exception) {
            LogUtils.error("Failed to start static server on port $port", e)
            throw IllegalStateException("Failed to start static server", e)
        }
    }

    /**
     * 停止服务器并清理资源。
     */
    fun stopServer() {
        server?.let {
            try {
                it.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
                LogUtils.info("Static server stopped")
            } catch (e: Exception) {
                LogUtils.error("Failed to stop server", e)
            } finally {
                server = null
            }
        }
    }

    companion object {
        val servers = mutableSetOf<M8TestDocsServer>()
    }
}