package eu.hxreborn.gboardmaterialexpressiveblack

import android.graphics.RenderEffect
import android.graphics.Shader
import android.inputmethodservice.InputMethodService
import android.os.Build
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
            
            // Ищем корневое View клавиатуры
            val inputView = runCatching {
                InputMethodService::class.java.getMethod("getCurrentInputView").invoke(service) as? View
            }.getOrNull() ?: return@intercept result

            // Создаем эффект размытия (радиус 30-40, можно менять)
            val blurEffect = RenderEffect.createBlurEffect(
                40f, 40f, Shader.TileMode.CLAMP
            )

            // Применяем эффект напрямую к контейнеру клавиатуры
            inputView.setRenderEffect(blurEffect)
            
            // Если у Gboard фон внутри View имеет свой цвет, его нужно убрать, 
            // иначе блюр будет "под" цветом и его не будет видно
            inputView.setBackgroundColor(0x00000000) // Полностью прозрачный
            
            // Если есть вложенные элементы, которые перекрывают, 
            // можно пройтись циклом и сделать их фоны прозрачными
            if (inputView is android.view.ViewGroup) {
                inputView.setBackgroundColor(0x00000000)
            }

            result
        }
    }
}
