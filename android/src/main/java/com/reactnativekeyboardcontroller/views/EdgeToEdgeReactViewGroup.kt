package com.reactnativekeyboardcontroller.views

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.views.view.ReactViewGroup
import com.reactnativekeyboardcontroller.extensions.content
import com.reactnativekeyboardcontroller.extensions.removeSelf
import com.reactnativekeyboardcontroller.extensions.requestApplyInsetsWhenAttached
import com.reactnativekeyboardcontroller.extensions.rootView
import com.reactnativekeyboardcontroller.listeners.KeyboardAnimationCallback
import com.reactnativekeyboardcontroller.listeners.KeyboardAnimationCallbackConfig
import com.reactnativekeyboardcontroller.log.Logger
import com.reactnativekeyboardcontroller.modal.ModalAttachedWatcher

private val TAG = EdgeToEdgeReactViewGroup::class.qualifiedName

@Suppress("detekt:TooManyFunctions")
@SuppressLint("ViewConstructor")
class EdgeToEdgeReactViewGroup(
  private val reactContext: ThemedReactContext,
) : ReactViewGroup(reactContext) {
  // props
  private var isStatusBarTranslucent = false
  private var isNavigationBarTranslucent = false
  private var isPreservingEdgeToEdge = false
  private var isEdgeToEdge = false
  var active: Boolean = false
    set(value) {
      field = value
      if (value) {
        enable()
      } else {
        disable()
      }
    }

  // internal class members
  private var eventView: ReactViewGroup? = null
  private var wasMounted = false
  private var callback: KeyboardAnimationCallback? = null
  private val config =
    KeyboardAnimationCallbackConfig(
      persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
      deferredInsetTypes = WindowInsetsCompat.Type.ime(),
      dispatchMode = WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE,
      hasTranslucentNavigationBar = isNavigationBarTranslucent,
    )

  // managers/watchers
  private val modalAttachedWatcher = ModalAttachedWatcher(this, reactContext, config, ::getKeyboardCallback)

  init {
    tag = VIEW_TAG
  }

  // region View life cycles
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    if (!wasMounted) {
      // skip logic with callback re-creation if it was first render/mount
      wasMounted = true
      return
    }

    this.activate()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    this.deactivate()
  }

  override fun onConfigurationChanged(newConfig: Configuration?) {
    this.reApplyWindowInsets()
  }
  // endregion

  // region State manager helpers
  private fun setupWindowInsets() {
    val rootView = reactContext.rootView
    if (rootView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
        val content = reactContext.content
        val params =
          FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
          )

        val shouldApplyZeroPaddingTop = !active || this.isStatusBarTranslucent
        val shouldApplyZeroPaddingBottom = !active || this.isNavigationBarTranslucent
        val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        params.setMargins(
          navBarInsets.left,
          if (shouldApplyZeroPaddingTop) {
            0
          } else {
            systemBarInsets.top
          },
          navBarInsets.right,
          if (shouldApplyZeroPaddingBottom) {
            0
          } else {
            navBarInsets.bottom
          },
        )
        content?.layoutParams = params

        // In recent versions of the react-native-keyboard-controller (>=1.8.0), the system default insets
        // are overridden. We reverted this back to the behavior from version 1.7.x and earlier, where this function
        // simply returns `insets`.
        // See the issue: https://github.com/kirillzyusko/react-native-keyboard-controller/issues/1013
        // Since we don't want the consequences of overriding and we don't use toggle enable/disable
        // nor react-navigation headers, which were the motivation behind the override,
        // it should be safe to return the original insets.
        // For reference:
        // 1.18.1: https://github.com/kirillzyusko/react-native-keyboard-controller/blob/1.18.1/android/src/main/java/com/reactnativekeyboardcontroller/views/EdgeToEdgeReactViewGroup.kt
        // 1.7.0: https://github.com/kirillzyusko/react-native-keyboard-controller/blob/1.7.0/android/src/main/java/com/reactnativekeyboardcontroller/views/EdgeToEdgeReactViewGroup.kt
        insets
      }
    }
  }

  fun setEdgeToEdge() {
    val nextValue = active || isPreservingEdgeToEdge

    if (isEdgeToEdge != nextValue) {
      isEdgeToEdge = nextValue

      reactContext.currentActivity?.let {
        WindowCompat.setDecorFitsSystemWindows(
          it.window,
          !isEdgeToEdge,
        )
      }
    }
  }

  private fun setupKeyboardCallbacks() {
    val activity = reactContext.currentActivity

    if (activity != null) {
      eventView = ReactViewGroup(context)
      val root = reactContext.content
      root?.addView(eventView)

      callback =
        KeyboardAnimationCallback(
          view = this,
          eventPropagationView = this,
          context = reactContext,
          config = config,
        )

      eventView?.let {
        ViewCompat.setWindowInsetsAnimationCallback(it, callback)
        ViewCompat.setOnApplyWindowInsetsListener(it, callback)
        it.requestApplyInsetsWhenAttached()
      }
    } else {
      Logger.w(TAG, "Can not setup keyboard animation listener, since `currentActivity` is null")
    }
  }

  private fun removeKeyboardCallbacks() {
    callback?.destroy()

    // capture view into closure, because if `onDetachedFromWindow` and `onAttachedToWindow`
    // dispatched synchronously after each other (open application on Fabric), then `.post`
    // will destroy just newly created view (if we have a reference via `this`)
    // and we'll have a memory leak or zombie-view
    val view = eventView
    // we need to remove view asynchronously from `onDetachedFromWindow` method
    // otherwise we may face NPE when app is getting opened via universal link
    // see https://github.com/kirillzyusko/react-native-keyboard-controller/issues/242
    // for more details
    Handler(Looper.getMainLooper()).post { view.removeSelf() }
  }

  private fun reApplyWindowInsets() {
    this.setupWindowInsets()
    this.requestApplyInsetsWhenAttached()
  }
  // endregion

  // region State managers
  private fun enable() {
    this.setupWindowInsets()
    this.activate()
  }

  private fun disable() {
    this.setupWindowInsets()
    this.deactivate()
  }

  private fun activate() {
    this.setupKeyboardCallbacks()
    modalAttachedWatcher.enable()
  }

  private fun deactivate() {
    this.removeKeyboardCallbacks()
    modalAttachedWatcher.disable()
  }
  // endregion

  // region Helpers
  private fun getKeyboardCallback(): KeyboardAnimationCallback? = this.callback
  // endregion

  // region Props setters
  fun setStatusBarTranslucent(isStatusBarTranslucent: Boolean) {
    this.isStatusBarTranslucent = isStatusBarTranslucent
  }

  fun setNavigationBarTranslucent(isNavigationBarTranslucent: Boolean) {
    this.isNavigationBarTranslucent = isNavigationBarTranslucent
    this.config.hasTranslucentNavigationBar = isNavigationBarTranslucent
  }

  fun setPreserveEdgeToEdge(isPreservingEdgeToEdge: Boolean) {
    this.isPreservingEdgeToEdge = isPreservingEdgeToEdge
  }
  // endregion

  // region external methods
  fun forceStatusBarTranslucent(isStatusBarTranslucent: Boolean) {
    if (active && this.isStatusBarTranslucent != isStatusBarTranslucent) {
      this.isStatusBarTranslucent = isStatusBarTranslucent
      this.reApplyWindowInsets()
    }
  }
  // endregion

  companion object {
    val VIEW_TAG = EdgeToEdgeReactViewGroup::class.simpleName
  }
}
