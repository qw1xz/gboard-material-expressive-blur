package eu.hxreborn.gboardmaterialexpressiveblack

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Gravity
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

            val inputView = runCatching {
                InputMethodService::class.java.getMethod("getCurrentInputView").invoke(service) as? View
            }.getOrNull() ?: return@intercept result

            // Функция, которая «пробивает» блюр в систему
            fun applyBlurState() {
                // Применяем флаги снова, так как изменение параметров их иногда сбрасывает
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setBackgroundBlurRadius(60)
                
                // Дополнительный вызов для безопасности
                val lp = window.attributes
                lp.blurBehindRadius = 60
                window.attributes = lp
            }

            // Функция изменения высоты с принудительным обновлением
            fun updateWindowHeight(height: Int) {
                if (height <= 0) return
                val lp = window.attributes
                lp.height = height
                lp.gravity = Gravity.BOTTOM
                window.attributes = lp
                
                // СРАЗУ ПОСЛЕ изменения высоты обновляем блюр
                applyBlurState()
            }

            // Первичная настройка
            updateWindowHeight(inputView.height)
            applyBlurState()

            // Слушатель изменений
            inputView.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
                val newHeight = bottom - top
                if (newHeight != (oldBottom - oldTop)) {
                    updateWindowHeight(newHeight)
                }
            }

            result
        }
    }
}
