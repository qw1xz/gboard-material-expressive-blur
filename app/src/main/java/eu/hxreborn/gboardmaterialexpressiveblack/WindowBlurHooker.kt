package eu.hxreborn.gboardmaterialexpressiveblack

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
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

            window.setBackgroundBlurRadius(60)

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val height = inputView.height
                    if (height > 0) {
                        val wm = service.getSystemService(
                            android.content.Context.WINDOW_SERVICE
                        ) as WindowManager
                        val lp = window.decorView.layoutParams
                            as WindowManager.LayoutParams
                        lp.height = height
                        wm.updateViewLayout(window.decorView, lp)
                    }
                } catch (_: Exception) {}
            }, 150)

            result
        }
    }
}
