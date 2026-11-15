package com.example.madomireader.data.source.extension

import android.app.Application
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.fullType

/**
 * Đăng ký các phụ thuộc mà extension lấy qua injekt.
 *
 * Các extension của Keiyoushi dùng `keiyoushi.utils`:
 * ```
 * val applicationContext: Application = Injekt.get()
 * val jsonInstance: Json = Injekt.get()
 * val protoInstance: ProtoBuf = Injekt.get()
 * ```
 * nên ứng dụng phải "cắm" sẵn ba giá trị này trước khi nạp extension đầu tiên.
 */
object InjektHost {

    @Volatile
    private var installed = false

    fun install(application: Application) {
        if (installed) return

        synchronized(this) {
            if (installed) return

            runCatching {
                Injekt.addSingleton(fullType<Application>(), application)
                Injekt.addSingleton(fullType<Json>(), json)
                Injekt.addSingleton(fullType<ProtoBuf>(), protoBuf)
                installed = true
            }.onFailure { error ->
                Log.e(TAG, "Không đăng ký được injekt: ${error.message}", error)
            }
        }
    }

    /** Cấu hình JSON dùng chung cho extension (bỏ qua khoá lạ, chấp nhận JSON thoáng). */
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val protoBuf: ProtoBuf = ProtoBuf

    private const val TAG = "InjektHost"
}
