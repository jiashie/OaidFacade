package com.jiashie.oaidfacade

internal class TimeoutOaidCallbackWrapper(private val orig: OaidCallback) : OaidCallback,
    TimeoutCheckable {
    @Volatile
    override var isDone = false
        private set

    @Volatile
    private var timeout = false
    override fun onTimeout() {
        log("getOaid timeout!!!")

        timeout = true
        orig.onOaidResult(199, "timeout", null, null, null)
    }

    override fun onOaidResult(code: Int, msg: String?, oaid: String?, vaid: String?, aaid: String?) {
        if (timeout) {
            log("re-call onGotOaid after timeout")
            return
        }
        isDone = true
        TimeoutChecker.remove(this)
        orig.onOaidResult(code, msg, oaid, vaid, aaid)
    }
}