package eu.hxreborn.gboardmaterialexpressiveblack

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Gravity
import io.github.libxposed.api.XposedModule

object WindowBlurHooker {
    fun hook(module: XposedModule) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val method = runCatching {
            InputMethodService::class.java
                .getDeclaredMethod("onWindowShown")
        }.getOrElse { return }

        module.hook(method).intercept { chain ->
            val result = chain.proceed()
            val service = chain.thisObject as? InputMethodService
                ?: return@intercept result
            val window = service.window?.window
                ?: return@intercept result
            val inputView = runCatching {
                InputMethodService::class.java
                    .getMethod("getCurrentInputView")
                    .invoke(service) as? android.view.View
            }.getOrNull() ?: return@intercept result

            inputView.post {
                val height = inputView.height
                if (height > 0) {
                    val params = window.attributes
                    params.height = height
                    params.gravity = Gravity.BOTTOM
                    window.attributes = params
                    window.setBackgroundBlurRadius(60)
                }
            }
            result
        }
    }
}
