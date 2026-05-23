package eu.hxreborn.gboardmaterialexpressiveblack

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.WindowManager
import io.github.libxposed.api.XposedModule

object WindowBlurHooker {
    fun hook(module: XposedModule) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val method = runCatching {
            InputMethodService::class.java.getDeclaredMethod("onWindowShown")
        }.getOrElse { return }

        module.hook(method).intercept { chain ->
            val result = chain.proceed()
            val service = chain.thisObject as? InputMethodService ?: return@intercept result
            val window = service.window?.window ?: return@intercept result
            
            // 1. Принудительная очистка фона окна, чтобы он не перекрывал блюр
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // 2. Включаем флаги размытия и убираем ограничения отрисовки
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            
            // 3. Устанавливаем радиус размытия
            window.setBackgroundBlurRadius(60)

            // 4. Очищаем фон самого DecorView (часто Gboard красит именно его)
            try {
                val decorView = window.decorView
                decorView.background = null
                decorView.setBackgroundColor(Color.TRANSPARENT)
            } catch (_: Exception) {}

            // 5. Обеспечиваем динамическое обновление, если Gboard меняет параметры окна
            val inputView = runCatching {
                InputMethodService::class.java.getMethod("getCurrentInputView").invoke(service) as? View
            }.getOrNull()

            inputView?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                try {
                    // Повторно применяем настройки при изменении лейаута
                    window.setBackgroundBlurRadius(60)
                    window.decorView.setBackgroundColor(Color.TRANSPARENT)
                } catch (_: Exception) {}
            }

            result
        }
    }
}
