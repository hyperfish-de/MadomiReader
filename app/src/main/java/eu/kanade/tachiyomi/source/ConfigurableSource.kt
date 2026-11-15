package eu.kanade.tachiyomi.source

import androidx.preference.PreferenceScreen

/**
 * Nguồn có màn hình cài đặt riêng (đăng nhập, đổi domain, bật/tắt tính năng…).
 *
 * Ứng dụng này cung cấp lớp [PreferenceScreen] thật của androidx.preference nên extension
 * có thể dựng màn hình cài đặt, nhưng hiện chưa hiển thị nó trong giao diện.
 */
@Suppress("Unused")
interface ConfigurableSource {

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
