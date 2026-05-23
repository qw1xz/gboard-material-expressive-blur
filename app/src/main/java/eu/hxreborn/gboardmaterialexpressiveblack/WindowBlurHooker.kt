package eu.hxreborn.gboardmaterialexpressiveblack

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Gravity
import android.view.View
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

            // 1. Включаем размытие фона окна
            window.setBackgroundBlurRadius(60)

            // Функция для обновления высоты окна
            fun updateWindowHeight(newHeight: Int) {
                if (newHeight <= 0) return
                val lp = window.attributes
                if (lp.height != newHeight) {
                    lp.height = newHeight
                    // КРИТИЧЕСКИ ВАЖНО: прижимаем обрезанное окно к низу экрана
                    lp.gravity = Gravity.BOTTOM 
                    // Присвоение attributes автоматически вызывает updateViewLayout под капотом
                    window.attributes = lp 
                }
            }

            // 2. Сразу применяем высоту, если View уже отрисована (чтобы избежать моргания на весь экран)
            updateWindowHeight(inputView.height)

            // 3. Динамически следим за изменениями размера без хардкодных задержек
            inputView.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
                val newHeight = bottom - top
                val oldHeight = oldBottom - oldTop

                if (newHeight != oldHeight) {
                    updateWindowHeight(newHeight)
                }
            }

            result
        }
    }
}
