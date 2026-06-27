package virtual.camera.app.view.base

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

open class BaseViewModel : ViewModel() {

    val errorLiveData = MutableLiveData<String>()

    fun launchOnIO(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    block()
                } catch (e: Throwable) {
                    Log.e("BaseViewModel", "Background task failed", e)
                    val msg = e.message ?: e.javaClass.simpleName
                    withContext(Dispatchers.Main) {
                        errorLiveData.value = msg
                    }
                }
            }
        }
    }

    /** Keep old name as alias so existing call-sites compile without changes. */
    fun launchOnUI(block: suspend CoroutineScope.() -> Unit) = launchOnIO(block)

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}
