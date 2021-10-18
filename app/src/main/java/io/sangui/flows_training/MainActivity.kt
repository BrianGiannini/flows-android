package io.sangui.flows_training

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.channels.Channel

class MainActivity : AppCompatActivity(), CoroutineScope {

    private val job: CompletableJob = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val flow = flow { // build
            for (i in 1..10) {
                emit(i) // transmit
                delay(300L)
            }
        }

        launch {
            flow.filter { // filter only these values
                it % 2 == 0
            }.map { // map value on other value
                it * it
            }.collect { // collect cold
                Log.d("FlowDebug", "flow for $it")
            }
        }

        val flowListInt = flowOf(1, 2, 3, 4) // same as asFlow()

        launch {
            flowListInt.collect { // cancelled when coroutine is cancelled
                Log.d("FlowDebug", "flowOf $it")
            }
        }

        val flowListInt2 = flowOf(1)
            .transform { emit("string now !") }

        launch {
            flowListInt2.collect {
                Log.d("FlowDebug", "flow to $it")
            }
        }

        val flow2 = flow { // build
            for (i in 1..10) {
                emit(i) // transmit
                delay(300L)
            }
        }

        launch {
            flow2.onEach {
                check(it != 7)
            }.catch { // catch errors
                    e ->
                Log.d("FlowDebug", "caught exception $e")
            }.onCompletion { // execute even if there is an error or not
                Log.d("FlowDebug", "flow completion")
            }.collect {
                Log.d("FlowDebug", "flow collect catch & completion")
            }
        }

        // TODO: flatMapConcat, flatMapLatest, flatMap, combine, flowOn


    }
}
