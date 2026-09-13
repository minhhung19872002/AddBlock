package com.hung.adskipper

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Nút "Màn hình đen" trong thanh cài đặt nhanh (kéo thanh thông báo xuống).
 *
 * Đây là cách bật tiện nhất khi đang xem YouTube: không phải rời YouTube, nên
 * video không bị dừng.
 */
class BlackScreenTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh(BlackScreenOverlay.isActive)
    }

    override fun onClick() {
        super.onClick()
        val service = AdSkipperService.instance
        if (service == null) {
            // Chưa bật quyền Trợ năng thì không phủ màn hình được — mở app để hướng dẫn.
            openApp()
            return
        }
        val turningOn = !BlackScreenOverlay.isActive
        service.toggleBlackScreen()
        refresh(turningOn)
    }

    private fun refresh(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (AdSkipperService.instance == null) {
            Tile.STATE_UNAVAILABLE
        } else if (active) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
