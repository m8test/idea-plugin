package com.m8test.idea.plugin.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.m8test.idea.plugin.config.M8TestSettings
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

object HttpUtils {
    private const val CONNECT_TIMEOUT_MS = 10_000L // 10 秒连接超时
    private const val REQUEST_TIMEOUT_MS = 30_000L // 30 秒请求超时
    private const val SOCKET_TIMEOUT_MS = 30_000L // 30 秒套接字超时

    private val client = HttpClient(CIO) {
        install(WebSockets)
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
    }

    private val settings by lazy { M8TestSettings.instance.state }
    private val gson = Gson()

    @Volatile
    private var isWebSocketConnected = false

    private var currentWebSocketSession: DefaultClientWebSocketSession? = null
    private var reconnectJob: Job? = null
    private val isReconnecting = AtomicBoolean(false)

    private const val maxRetries = 5
    private const val reconnectDelayMillis = 3000L


    data class Response(
        @SerializedName("success")
        val success: Boolean,
        @SerializedName("message")
        val message: String,
        @SerializedName("data")
        val data: Any?
    )

    suspend fun startProject(argument: String?) {
        val command = getCommand(type = "start", argument = argument?.let { it.ifBlank { null } })
        executeCommand(command)
    }

    suspend fun interruptProject() {
        val command = getCommand(type = "interrupt")
        executeCommand(command)
    }

    private suspend fun getCommand(type: String, argument: String? = null): String {
        val remotePath = getProjectRoot()
        // 基础命令
        var command = "script $type build --path $remotePath"
        // 如果 argument 存在，则添加 --argument 部分
        if (!argument.isNullOrBlank()) {
            command += " --argument $argument"
        }
        return command
    }

    private suspend fun executeCommand(command: String) {
        val url = buildHttpUrl("/command/execute")
        try {
            val response = client.post(url) { setBody(command) }.bodyAsText()
            val res = gson.fromJson(response, Response::class.java)
            if (res.success)
                LogUtils.debug("运行${command}成功")
            else
                LogUtils.error("运行${command}失败:${res.message}")
        } catch (e: Exception) {
            val msg = "获取项目根目录失败: ${e.message}"
            LogUtils.error(msg)
        }
    }

    suspend fun getProjectRoot(): String {
        val url = buildHttpUrl("/config/root")
        return try {
            val response = client.post(url).bodyAsText()
            LogUtils.debug("获取项目根目录成功: $response")
            response
        } catch (e: Exception) {
            val msg = "获取项目根目录失败: ${e.message}"
            LogUtils.error(msg)
            throw Exception(msg)
        }
    }

    fun buildWebSocketUrl(): String {
        return buildAddress("ws", "/console")
    }

    private fun buildHttpUrl(path: String): String {
        return buildAddress("http", path)
    }

    private fun buildAddress(protocol: String, path: String): String {
        val baseUrl = if (settings.enableAdbForwarding) {
            "localhost:${settings.debugPort}"
        } else {
            "${settings.deviceIp}:${settings.debugPort}"
        }
        return "$protocol://$baseUrl$path"
    }

    suspend fun connectWebSocket(
        onMessage: suspend (Entry) -> Unit,
        onError: suspend (Throwable) -> Unit
    ): Pair<Boolean, String> {
        if (isWebSocketConnected) {
            LogUtils.debug("WebSocket 已连接，跳过重复连接。")
            return true to "WebSocket 已连接"
        }

        val wsUrl = buildWebSocketUrl()
        try {
            LogUtils.debug("尝试连接 WebSocket: $wsUrl")
            client.ws(wsUrl) {
                isWebSocketConnected = true
                isReconnecting.set(false)
                currentWebSocketSession = this
                LogUtils.debug("已连接 WebSocket: $wsUrl")

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            try {
                                val text = frame.readText()
                                val entry = gson.fromJson(text, Entry::class.java)
                                onMessage(entry)
                                LogUtils.appendScriptLog(entry)
                            } catch (e: Exception) {
                                LogUtils.error("消息解析失败: ${e.message}")
                                onError(e)
                            }
                        }
                    }
                } finally {
                    isWebSocketConnected = false
                    currentWebSocketSession = null
                    LogUtils.warn("WebSocket 连接已断开，启动重连 $wsUrl")
                    launchReconnect(onMessage, onError)
                }
            }
            return true to "WebSocket 连接成功"
        } catch (e: Exception) {
            isWebSocketConnected = false
            val errorMsg = when (e) {
                is io.ktor.client.network.sockets.ConnectTimeoutException -> {
                    "WebSocket 连接超时: ${e.message} (URL: $wsUrl, 超时时间: ${CONNECT_TIMEOUT_MS}ms)"
                }

                else -> "WebSocket 连接失败: ${e.message} (URL: $wsUrl)"
            }
            LogUtils.error(errorMsg)
            onError(e)
            launchReconnect(onMessage, onError)
            return false to errorMsg
        }
    }

    // 自动重连逻辑
    private fun launchReconnect(
        onMessage: suspend (Entry) -> Unit,
        onError: suspend (Throwable) -> Unit
    ) {
        if (isReconnecting.getAndSet(true)) return // 防止并发重连

        ApplicationManager.getApplication().executeOnPooledThread {
            var retryCount = 0
            while (!isWebSocketConnected && retryCount < maxRetries) {
                Thread.sleep(reconnectDelayMillis)
                LogUtils.info("正在尝试第 ${retryCount + 1} 次重连...")
                try {
                    val (success, message) = runBlocking { connectWebSocket(onMessage, onError) }
                    if (success) {
                        LogUtils.debug("WebSocket 重连成功")
                        return@executeOnPooledThread
                    } else {
                        LogUtils.info("第 ${retryCount + 1} 次重连失败: $message")
                    }
                } catch (e: Exception) {
                    val errorMsg = when (e) {
                        is io.ktor.client.network.sockets.ConnectTimeoutException -> {
                            "第 ${retryCount + 1} 次重连超时: ${e.message}"
                        }

                        else -> "第 ${retryCount + 1} 次重连失败: ${e.message}"
                    }
                    LogUtils.error(errorMsg)
                }
                retryCount++
            }
            LogUtils.info("WebSocket 重连失败，已达到最大重试次数 ($maxRetries)。")
            isReconnecting.set(false)
        }
    }

    suspend fun disconnectWebSocket() {
        try {
            reconnectJob?.cancel()
            reconnectJob = null
            currentWebSocketSession?.close()
            isWebSocketConnected = false
            LogUtils.info("WebSocket 已手动断开")
        } catch (e: Exception) {
            LogUtils.error("WebSocket 手动断开失败: ${e.message}")
        } finally {
            currentWebSocketSession = null
        }
    }

    suspend fun close() {
        client.close()
        isWebSocketConnected = false
        reconnectJob?.cancel()
        LogUtils.verbose("HTTP 客户端已关闭")
    }

    fun connectWebSocketAsync(onResult: (Boolean, String) -> Unit) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "连接 WebSocket 日志", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在连接 WebSocket..."
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val (success, message) = runBlocking {
                            connectWebSocket(
                                onMessage = { entry ->
//                                    logger.info("WebSocket message: [${entry.level}] ${entry.tag}: ${entry.message}")
                                },
                                onError = { throwable ->
//                                    logger.error("WebSocket error: ${throwable.message}", throwable)
                                }
                            )
                        }
                        if (!success) {
                            LogUtils.error("connectWebSocketAsync: 连接失败 - $message")
                        }
                        if (!isWebSocketConnected) {
                            LogUtils.error("connectWebSocketAsync: 连接状态异常，未建立连接")
                        }
                        LogUtils.info("connectWebSocketAsync: 连接成功")
                        onResult(success, message)
                    } catch (e: Exception) {
                        val errorMsg = when (e) {
                            is io.ktor.client.network.sockets.ConnectTimeoutException -> {
                                "WebSocket 连接超时: ${e.message} (超时时间: ${CONNECT_TIMEOUT_MS}ms)"
                            }

                            else -> "WebSocket 连接失败: ${e.message}"
                        }
                        LogUtils.error("connectWebSocketAsync: $errorMsg")
                        onResult(false, errorMsg)
                    }
                }
            }
        })
    }

    data class Entry(
        val id: Int,
        val level: String,
        val tag: String,
        val message: String,
        val time: String
    )
}
