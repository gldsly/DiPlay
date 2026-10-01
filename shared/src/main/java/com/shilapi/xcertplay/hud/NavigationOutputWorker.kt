package com.shilapi.xcertplay.hud

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Each output owns a worker: a blocked vendor service cannot block the phone or the other output. */
internal class NavigationOutputWorker(
    private val name: String,
    private val clearOutput: () -> Unit,
    private val report: (String) -> Unit = {},
) {
    private val executor = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue<Runnable>(128), { task -> Thread(task, name).apply { isDaemon = true } })
    private var accepting = false

    @Synchronized fun start(initialize: () -> Unit) {
        if (accepting) return
        accepting = true
        executor.queue.clear()
        executor.execute { runCatching { clearOutput(); initialize() } }
    }

    @Synchronized fun submit(action: () -> Unit) {
        if (!accepting) return
        try {
            executor.execute { runCatching(action) }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            // Incremental protocol frames cannot safely be dropped individually, so this output stays
            // down until the next session. Say so: it used to stop silently, leaving a blank guidance
            // display with nothing in the diagnostic report to explain it.
            report("$name: output stopped, the vendor service is behind and frames cannot be dropped safely")
            clear()
        }
    }

    @Synchronized fun clear() {
        accepting = false
        executor.queue.clear()
        // The queue was just emptied, but the pool can still refuse; a failure here would escape into
        // the frame callback that triggered the stop.
        runCatching { executor.execute { runCatching(clearOutput) } }
    }
}
