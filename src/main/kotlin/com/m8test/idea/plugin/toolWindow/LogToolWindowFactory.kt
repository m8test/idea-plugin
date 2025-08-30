package com.m8test.idea.plugin.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.jetbrains.rd.util.AtomicInteger
import com.m8test.idea.plugin.util.WebSocketUtils
import com.m8test.idea.plugin.util.LogUtils
import java.awt.*
import javax.swing.*
import javax.swing.text.BadLocationException
import javax.swing.text.Style
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

class LogToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val logPanel = LogPanel(project)
        LogUtils.setLogPanel(logPanel)
        val content = ContentFactory.getInstance().createContent(logPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    class LogPanel(project: Project) : JPanel() {
        private val textPaneScript: JTextPane = JTextPane()
        private val docScript: StyledDocument = textPaneScript.styledDocument
        private val textPanePlugin: JTextPane = JTextPane()
        private val docPlugin: StyledDocument = textPanePlugin.styledDocument

        private val logEntriesScript = mutableListOf<LogEntry>() // 存储全部日志用于过滤
        private val logEntriesPlugin = mutableListOf<LogEntry>() // 存储全部日志用于过滤

        private var filterLevel: String = "ALL"
        private var searchQuery: String = "" // 搜索的关键字

        private data class LogEntry(val text: String, val level: String, val color: Color)

        init {
            textPaneScript.isEditable = false
            textPaneScript.contentType = "text/plain"
            textPanePlugin.isEditable = false
            textPanePlugin.contentType = "text/plain"

            // 下拉菜单
            val levelComboBox = ComboBox(
                arrayOf(
                    "ALL",
                    LogUtils.LEVEL_DEBUG,
                    LogUtils.LEVEL_VERBOSE,
                    LogUtils.LEVEL_INFO,
                    LogUtils.LEVEL_WARN,
                    LogUtils.LEVEL_ERROR,
                    LogUtils.LEVEL_ASSERT
                )
            ).apply {
                selectedItem = "ALL"
                addActionListener {
                    val selected = selectedItem.toString().uppercase()
                    filterLevel = selected
                    reloadLogs()
                }
            }

            // 清空按钮
            val clearButton = JButton("清空日志").apply {
                addActionListener {
                    ApplicationManager.getApplication().invokeLater {
                        logEntriesScript.clear()
                        docScript.remove(0, docScript.length)
                        logEntriesPlugin.clear()
                        docPlugin.remove(0, docPlugin.length)
                    }
                }
            }

            // 搜索框
            val searchField = JTextField().apply {
                preferredSize = Dimension(200, 25)
                addActionListener {
                    searchQuery = text
                    reloadLogs() // 每次输入后重新加载日志
                }
            }

            // 顶部面板，使用 GridBagLayout
            val topPanel = JPanel(GridBagLayout()).apply {
                val constraints = GridBagConstraints()
                constraints.fill = GridBagConstraints.HORIZONTAL

                // 日志等级标签
                constraints.gridx = 0
                constraints.weightx = 0.0
                add(JLabel("日志等级:"), constraints)

                // 日志等级下拉框
                constraints.gridx = 1
                constraints.weightx = 1.0
                add(levelComboBox, constraints)

                // 搜索框标签
                constraints.gridx = 2
                constraints.weightx = 0.0
                add(JLabel("搜索:"), constraints)

                // 搜索框
                constraints.gridx = 3
                constraints.weightx = 1.0
                add(searchField, constraints)

                // 清空按钮
                constraints.gridx = 4
                constraints.weightx = 0.0
                add(clearButton, constraints)
            }

            val scrollPaneScript = JBScrollPane(textPaneScript).apply {
                verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }

            val scrollPanePlugin = JBScrollPane(textPanePlugin).apply {
                verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }

            // 添加 JTabbedPane
            val tabbedPane = JBTabbedPane()
            tabbedPane.addTab("脚本日志", scrollPaneScript)
            tabbedPane.addTab("插件日志", scrollPanePlugin)

            layout = BorderLayout()
            add(topPanel, BorderLayout.NORTH)
            add(tabbedPane, BorderLayout.CENTER)

            minimumSize = Dimension(300, 200)
            textPaneScript.minimumSize = Dimension(300, 200)
            textPanePlugin.minimumSize = Dimension(300, 200)
            scrollPaneScript.minimumSize = Dimension(300, 200)
            scrollPanePlugin.minimumSize = Dimension(300, 200)
        }

        /**
         * 公共方法：处理日志相关操作，包括生成前缀、颜色、创建LogEntry对象并添加到列表，以及根据条件展示日志
         */
        private fun processLog(
            entry: WebSocketUtils.Entry,
            logEntries: MutableList<LogEntry>,
            textPane: JTextPane,
            doc: StyledDocument
        ) {
            val prefix = when (entry.level.uppercase()) {
                LogUtils.LEVEL_DEBUG -> "[DEBUG] ${entry.tag}: "
                LogUtils.LEVEL_VERBOSE -> "[VERBOSE] ${entry.tag}: "
                LogUtils.LEVEL_INFO -> "[INFO ] ${entry.tag}: "
                LogUtils.LEVEL_WARN -> "[WARN ] ${entry.tag}: "
                LogUtils.LEVEL_ERROR -> "[ERROR] ${entry.tag}: "
                LogUtils.LEVEL_ASSERT -> "[ASSERT] ${entry.tag}: "
                else -> "[UNKWN] ${entry.tag}: "
            }
            val color = when (entry.level.uppercase()) {
                LogUtils.LEVEL_DEBUG -> Color(176, 176, 176) // 浅灰色
                LogUtils.LEVEL_VERBOSE -> Color(169, 169, 169) // 中灰色
                LogUtils.LEVEL_INFO -> Color(0, 0, 0) // 黑色
                LogUtils.LEVEL_WARN -> Color(255, 165, 0) // 橙色
                LogUtils.LEVEL_ERROR -> Color(255, 0, 0) // 红色
                LogUtils.LEVEL_ASSERT -> Color(30, 144, 255) // 蓝色
                else -> Color(169, 169, 169) // 默认灰色
            }

            val fullText = "${entry.time} $prefix${entry.message}\n"

            val entryObj = LogEntry(fullText, entry.level.uppercase(), color)
            logEntries.add(entryObj)

            // 若当前等级符合过滤器要求，则添加
            if (shouldDisplay(entryObj.level)) {
                appendStyledText(textPane, doc, entryObj.text, entryObj.color)
            }
        }

        fun appendScriptLog(entry: WebSocketUtils.Entry) {
            processLog(entry, logEntriesScript, textPaneScript, docScript)
        }

        fun appendPluginLog(entry: WebSocketUtils.Entry) {
            processLog(entry, logEntriesPlugin, textPanePlugin, docPlugin)
        }

        private fun formatTime(time: Long): String {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            return formatter.format(time)
        }

        private val logId = AtomicInteger(1)
        fun appendPluginLog(level: String, message: String) {
            appendPluginLog(
                WebSocketUtils.Entry(
                    level = level,
                    message = message,
                    id = logId.getAndIncrement(),
                    time = formatTime(System.currentTimeMillis()),
                    tag = "M8Test"
                )
            )
        }

        /**
         * 内部添加带颜色的日志到视图
         */
        private fun appendStyledText(textPane: JTextPane, doc: StyledDocument, text: String, color: Color) {
            ApplicationManager.getApplication().invokeLater {
                val style: Style = textPane.addStyle("LogStyle", null)
                StyleConstants.setForeground(style, color)
                try {
                    doc.insertString(doc.length, text, style)
                    textPane.caretPosition = doc.length
                    textPane.repaint()
                } catch (e: BadLocationException) {
                }
            }
        }

        /**
         * 是否显示该日志等级
         */
        private fun shouldDisplay(level: String): Boolean {
            return filterLevel == "ALL" || level == filterLevel
        }

        /**
         * 刷新所有日志，根据当前过滤等级和搜索内容重新加载
         */
        private fun reloadLogs() {
            ApplicationManager.getApplication().invokeLater {
                docScript.remove(0, docScript.length)
                docPlugin.remove(0, docPlugin.length)
                for (entry in logEntriesScript) {
                    if (shouldDisplay(entry.level) && (searchQuery.isEmpty() || entry.text.contains(
                            searchQuery,
                            ignoreCase = true
                        ))
                    ) {
                        appendStyledText(textPaneScript, docScript, entry.text, entry.color)
                    }
                }
                for (entry in logEntriesPlugin) {
                    if (shouldDisplay(entry.level) && (searchQuery.isEmpty() || entry.text.contains(
                            searchQuery,
                            ignoreCase = true
                        ))
                    ) {
                        appendStyledText(textPanePlugin, docPlugin, entry.text, entry.color)
                    }
                }
            }
        }
    }
}