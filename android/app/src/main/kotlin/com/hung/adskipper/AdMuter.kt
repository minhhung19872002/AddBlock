package com.hung.adskipper

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log

/**
 * Tắt/mở tiếng luồng nhạc trong lúc quảng cáo chạy.
 *
 * Ưu tiên ADJUST_MUTE vì hệ thống tự nhớ mức âm lượng cũ. Máy nào chặn (một số
 * ROM ném SecurityException) thì lùi về cách thủ công: nhớ mức cũ, kéo về 0,
 * xong trả lại.
 */
class AdMuter(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager: AudioManager? =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var muted = false
    private var usedFallback = false
    private var previousVolume = -1
    private var mutedAt = 0L

    val isMuted: Boolean get() = muted

    /** Đã tắt tiếng quá lâu chưa — chốt chặn cuối cùng phòng khi nhận diện sai. */
    fun isStuck(maxMutedMs: Long): Boolean =
        muted && SystemClock.uptimeMillis() - mutedAt > maxMutedMs

    fun mute(): Boolean {
        val manager = audioManager ?: return false
        if (muted) return false

        val viaAdjust = runCatching {
            manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
        }.isSuccess

        if (!viaAdjust) {
            val fallback = runCatching {
                previousVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            }.isSuccess
            if (!fallback) {
                Log.w(TAG, "Không tắt tiếng được — hệ thống từ chối")
                return false
            }
            usedFallback = true
        } else {
            usedFallback = false
        }

        muted = true
        mutedAt = SystemClock.uptimeMillis()
        return true
    }

    fun unmute(): Boolean {
        val manager = audioManager ?: return false
        if (!muted) return false

        runCatching {
            if (usedFallback) {
                if (previousVolume >= 0) {
                    manager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
                }
            } else {
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0,
                )
            }
        }

        muted = false
        usedFallback = false
        previousVolume = -1
        return true
    }

    private companion object {
        const val TAG = "AdSkipper"
    }
}
