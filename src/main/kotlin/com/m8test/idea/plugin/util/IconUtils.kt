package com.m8test.idea.plugin.util

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import javax.swing.Icon
import javax.swing.ImageIcon

object IconUtils {
    // 加载并缩放图标
    fun loadScaledIcon(path: String, size: Int): Icon {
        try {
            val originalIcon = IconLoader.getIcon(path, IconUtils::class.java)
            return IconUtil.scale(originalIcon, null, size.toFloat() / originalIcon.iconWidth)
        } catch (e: Exception) {
            Messages.showErrorDialog("无法加载图标: $path", "图标加载错误")
            return ImageIcon()
        }
    }
}