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

            // ДОБАВЛЕНО: Принудительно включаем флаг размытия фона
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            
            // Оставляем этот метод, он работает в паре с флагом на некоторых прошивках
            window.setBackgroundBlurRadius(60) 

            fun updateWindowHeight(newHeight: Int) {
                if (newHeight <= 0) return
                val lp = window.attributes
                var needsUpdate = false

                if (lp.height != newHeight) {
                    lp.height = newHeight
                    lp.gravity = Gravity.BOTTOM
                    needsUpdate = true
                }

                // ДОБАВЛЕНО: Передаем радиус напрямую в LayoutParams
                if (lp.blurBehindRadius != 60) {
                    lp.blurBehindRadius = 60
                    needsUpdate = true
                }

                if (needsUpdate) {
                    window.attributes = lp
                }
            }

            updateWindowHeight(inputView.height)

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
