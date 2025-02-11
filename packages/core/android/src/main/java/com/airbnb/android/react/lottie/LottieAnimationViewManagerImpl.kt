package com.airbnb.android.react.lottie

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.RenderMode
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.util.RNLog
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import kotlin.concurrent.thread

internal object LottieAnimationViewManagerImpl {
    const val REACT_CLASS = "LottieAnimationView"
    val coroutineScope = CoroutineScope(Dispatchers.Main)

    @JvmStatic
    val exportedViewConstants: Map<String, Any>
        get() = MapBuilder.builder<String, Any>()
            .put("VERSION", 1)
            .build()

    @JvmStatic
    fun createViewInstance(context: ThemedReactContext): LottieAnimationView {
        return LottieAnimationView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
    }

    @JvmStatic
    fun getExportedCustomBubblingEventTypeConstants(): MutableMap<String, Any> {
        return MapBuilder.of(
            "animationFinish",
            MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onAnimationFinish"))
        )
    }

    @JvmStatic
    fun play(view: LottieAnimationView, startFrame: Int, endFrame: Int) {
        Handler(Looper.getMainLooper()).post {
            if (startFrame != -1 && endFrame != -1) {
                if (startFrame > endFrame) {
                    view.setMinAndMaxFrame(endFrame, startFrame)
                    if (view.speed > 0) {
                        view.reverseAnimationSpeed()
                    }
                } else {
                    view.setMinAndMaxFrame(startFrame, endFrame)
                    if (view.speed < 0) {
                        view.reverseAnimationSpeed()
                    }
                }
            }
            if (ViewCompat.isAttachedToWindow(view)) {
                view.progress = 0f
                view.playAnimation()
            } else {
                view.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        val listenerView = v as LottieAnimationView
                        listenerView.progress = 0f
                        listenerView.playAnimation()
                        listenerView.removeOnAttachStateChangeListener(this)
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        val listenerView = v as LottieAnimationView
                        listenerView.removeOnAttachStateChangeListener(this)
                    }
                })
            }
        }
    }

    @JvmStatic
    fun reset(view: LottieAnimationView) {
        Handler(Looper.getMainLooper()).post {
            if (ViewCompat.isAttachedToWindow(view)) {
                view.cancelAnimation()
                view.progress = 0f
            }
        }
    }

    @JvmStatic
    fun pause(view: LottieAnimationView) {
        Handler(Looper.getMainLooper()).post {
            if (ViewCompat.isAttachedToWindow(view)) {
                view.pauseAnimation()
            }
        }
    }

    @JvmStatic
    fun resume(view: LottieAnimationView) {
        Handler(Looper.getMainLooper()).post {
            if (ViewCompat.isAttachedToWindow(view)) {
                view.resumeAnimation()
            }
        }
    }

    @JvmStatic
    fun setSourceName(
        name: String?,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        // To match the behaviour on iOS we expect the source name to be
        // extensionless. This means "myAnimation" corresponds to a file
        // named `myAnimation.json` in `main/assets`. To maintain backwards
        // compatibility we only add the .json extension if no extension is
        // passed.
        var resultSourceName = name
        if (resultSourceName?.contains(".") == false) {
            resultSourceName = "$resultSourceName.json"
        }
        viewManager.animationName = resultSourceName
    }

    @JvmStatic
    fun setSourceJson(
        json: String?,
        propManagersMap: LottieAnimationViewPropertyManager
    ) {
        propManagersMap.animationJson = json
    }

    @JvmStatic
    fun setSourceURL(
        urlString: String?,
        propManagersMap: LottieAnimationViewPropertyManager
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    URL(urlString).openStream().use {
                        BufferedReader(InputStreamReader(it)).useLines { lines ->
                            lines.joinToString("\n")
                        }
                    }
                }
                propManagersMap.animationJson = jsonString
                propManagersMap.commitChanges()
            } catch (e: Exception) {
                RNLog.l("Error while loading animation from URL")
            }
        }
    }

    @JvmStatic
    fun setCacheComposition(view: LottieAnimationView, cacheComposition: Boolean) {
        view.setCacheComposition(cacheComposition)
    }

    @JvmStatic
    fun setResizeMode(
        resizeMode: String?,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        var mode: ImageView.ScaleType? = null
        when (resizeMode) {
            "cover" -> {
                mode = ImageView.ScaleType.FIT_XY
            }
            "contain" -> {
                mode = ImageView.ScaleType.CENTER_INSIDE
            }
            "center" -> {
                mode = ImageView.ScaleType.CENTER
            }
        }
        viewManager.scaleType = mode
    }

    @JvmStatic
    fun setRenderMode(
        renderMode: String?,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        var mode: RenderMode? = null
        when (renderMode) {
            "AUTOMATIC" -> {
                mode = RenderMode.AUTOMATIC
            }
            "HARDWARE" -> {
                mode = RenderMode.HARDWARE
            }
            "SOFTWARE" -> {
                mode = RenderMode.SOFTWARE
            }
        }
        viewManager.renderMode = mode
    }

    @JvmStatic
    fun setHardwareAcceleration(
        hardwareAccelerationAndroid: Boolean,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        var layerType: Int? = View.LAYER_TYPE_SOFTWARE
        if (hardwareAccelerationAndroid) {
            layerType = View.LAYER_TYPE_HARDWARE
        }
        viewManager.layerType = layerType
    }

    @JvmStatic
    fun setProgress(
        progress: Float,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        viewManager.progress = progress
    }

    @JvmStatic
    fun setSpeed(
        speed: Double,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        viewManager.speed = speed.toFloat()
    }

    @JvmStatic
    fun setLoop(
        loop: Boolean,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        viewManager.loop = loop
    }

    @JvmStatic
    fun setAutoPlay(
        autoPlay: Boolean,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        viewManager.autoPlay = autoPlay
    }

    @JvmStatic
    fun setEnableMergePaths(
        enableMergePaths: Boolean,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        viewManager.enableMergePaths = enableMergePaths
    }

    @JvmStatic
    fun setImageAssetsFolder(
        imageAssetsFolder: String?,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        viewManager.imageAssetsFolder = imageAssetsFolder
    }

    @JvmStatic
    fun setColorFilters(
        colorFilters: ReadableArray?,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        viewManager.colorFilters = colorFilters
    }

    @JvmStatic
    fun setTextFilters(
        textFilters: ReadableArray?,
        viewManager: LottieAnimationViewPropertyManager
    ) {
        viewManager.textFilters = textFilters
    }
}