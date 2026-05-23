package eu.hxreborn.gboardmaterialexpressiveblack

import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
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
            val window = service.window?.window ?: return@intercept result
            
            val inputView = runCatching {
                InputMethodService::class.java.getMethod("getCurrentInputView").invoke(service) as? View
            }.getOrNull() ?: return@intercept result

            // 1. Включаем блюр (он будет работать только внутри заданного нами Outline)
            window.setBackgroundBlurRadius(60)

            // Массив для вычисления координат клавиатуры на экране (выносим, чтобы не нагружать память)
            val location = IntArray(2)

            // 2. Создаем прозрачный фон с умным контуром
            val customBackground = object : ColorDrawable(Color.TRANSPARENT) {
                override fun getOutline(outline: Outline) {
                    if (inputView.width > 0 && inputView.height > 0) {
                        // Получаем точные координаты inputView внутри окна
                        inputView.getLocationInWindow(location)
                        val left = location[0]
                        val top = location[1]
                        val right = left + inputView.width
                        val bottom = top + inputView.height
                        
                        // Задаем контур строго по границам клавиатуры
                        // Если у Gboard закругленные верхние углы, можно использовать:
                        // outline.setRoundRect(left, top, right, bottom, 24f) // где 24f - радиус углов
                        outline.setRect(left, top, right, bottom)
                        
                        // Обязательно для SurfaceFlinger, иначе контур будет проигнорирован
                        outline.alpha = 1.0f 
                    } else {
                        super.getOutline(outline)
                    }
                }
            }

            // 3. Подменяем фон окна на наш кастомный
            window.setBackgroundDrawable(customBackground)
            
            // Отключаем обрезку контента по контуру, чтобы всплывающие буквы при удержании не обрезались
            window.decorView.clipToOutline = false

            // 4. Слушаем любые изменения размеров или положения клавиатуры (открытие эмодзи, сдвиг)
            inputView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                    // Перерисовываем контур блюра под новые размеры
                    window.decorView.invalidateOutline()
                }
            }

            result
        }
    }
}
