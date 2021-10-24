package io.sangui.flows_training

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import timber.log.Timber

// Original source: https://github.com/Zhuinden/fragmentviewbindingdelegate-kt
// Changed to return null instead of throwing when attempting to get binding when Fragment views are destroyed.
// Add a way to cleanup the binding when Fragment views are destroyed.
private class FragmentViewBindingDelegate<T : ViewBinding>(
    private val fragment: Fragment,
    private val viewBindingFactory: (View) -> T,
    private val viewBindingCleanup: (T.() -> Unit)? = null
) : ReadOnlyProperty<Fragment, T?> {

    private var binding: T? = null
    private val fragmentName by lazy { fragment::class.java.simpleName }

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            val viewLifecycleOwnerLiveDataObserver =
                Observer<LifecycleOwner?> {
                    val viewLifecycleOwner = it ?: return@Observer

                    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            val safeBinding = binding
                            if (viewBindingCleanup != null && safeBinding != null) {
                                viewBindingCleanup.invoke(safeBinding)
                            }

                            binding = null
                        }
                    })
                }

            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observeForever(viewLifecycleOwnerLiveDataObserver)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.removeObserver(viewLifecycleOwnerLiveDataObserver)
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? {
        val safeBinding = this.binding
        if (safeBinding != null) {
            return safeBinding
        }

        // Check fragment view exists, as fragment.viewLifecycleOwner would throw if not.
        val view = thisRef.view
        if (view == null) {
            warnViewsAreDestroyed()
            return null
        }

        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            warnViewsAreDestroyed()
            return null
        }

        return viewBindingFactory(view).also { this.binding = it }
    }

    private fun warnViewsAreDestroyed() {
        Timber.w("Attempting to get binding when fragment views are destroyed ($fragmentName).")
    }
}

/**
 * Creates a [ViewBinding] for this [Fragment], that will automatically remove itself when the [Fragment] view is destroyed.
 * This methods provides a lambda to cleanup the [ViewBinding], which will be called just before [Fragment.onDestroyView].
 *
 * Notice that the provided [ViewBinding] will be `null` in [Fragment.onDestroyView] and any attempt to use it will log a warning.
 */
fun <T : ViewBinding> Fragment.viewBinding(viewBindingFactory: (View) -> T, viewBindingCleanup: (T.() -> Unit)? = null): ReadOnlyProperty<Fragment, T?> =
    FragmentViewBindingDelegate(this, viewBindingFactory, viewBindingCleanup)

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(crossinline bindingInflater: (LayoutInflater) -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(layoutInflater)
    }
