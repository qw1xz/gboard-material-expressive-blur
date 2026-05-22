package eu.hxreborn.gboardmaterialexpressiveblack

import android.inputmethodservice.InputMethodService
import android.os.Build
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
            window.setBackgroundBlurRadius(60)
            result
        }
    }
}
