package com.jiashie.oaidfacade

import android.content.Context
import android.util.Log
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object OaidFacade {
    internal val IMPL: OaidSdk by lazy {
        val impl: OaidSdk = try {
            OaidSdkImplV1026.tryCreate() ?: OaidSdkImplV1025.tryCreate() ?: OaidSdkImplStub()
        } catch (t: Throwable) {
            t.printStackTrace()
            OaidSdkImplStub()
        }
        log("impl=${impl}")
        impl
    }

    private var contextRef: WeakReference<Context>? = null

    private val cacheable: Cacheable<CachedResult> = object : Cacheable<CachedResult>() {
        override fun fetchData(): CachedResult? {
            try {
                val context = contextRef?.get() ?: return null

                if (FindEmulator.isEmulator(context)) {
                    return CachedResult(199, "emulator not supported")
                }

                val latch = CountDownLatch(1)
                val result = Array<CachedResult?>(1) { null }

                val callback = object : OaidCallback {
                    override fun onOaidResult(
                        code: Int,
                        msg: String?,
                        oaid: String?,
                        vaid: String?,
                        aaid: String?,
                    ) {
                        result[0] = CachedResult(code, msg, oaid, vaid, aaid)
                        latch.countDown()
                    }

                }
                val handler = IdentifierListenerHandler(callback)

                val code: Int = IMPL.initSdk(context, handler)
                when (code) {
                    ErrorCode.INIT_ERROR_DEVICE_NOSUPPORT ->
                        //不支持的设备
                        callback.onOaidResult(code, "device not support", null, null, null)
                    ErrorCode.INIT_ERROR_LOAD_CONFIGFILE ->
                        //加载配置文件出错
                        callback.onOaidResult(code, "error load config file", null, null, null)
                    ErrorCode.INIT_ERROR_MANUFACTURER_NOSUPPORT ->
                        //不支持的设备厂商
                        callback.onOaidResult(code, "manufacturer not support", null, null, null)
                    ErrorCode.INIT_ERROR_RESULT_DELAY -> {
                        //获取接口是异步的，结果会在回调中返回，回调执行的回调可能在工作线程
                        // 添加超时检测回调
                        val wrapper = TimeoutOaidCallbackWrapper(callback)
                        handler.callback = wrapper
                        TimeoutChecker.check(3000, wrapper)
                    }
                    ErrorCode.INIT_HELPER_CALL_ERROR ->
                        //反射调用出错
                        callback.onOaidResult(code, "reflect call error", null, null, null)
                    ErrorCode.INIT_ERROR_BEGIN -> {} //获取接口是同步的，SDK内部会回调onSupport
                    ErrorCode.INIT_ERROR_CERT_ERROR -> {
                        //证书错误，且重置过，不缓存结果，需要再次调用initSdk
                        return null
                    }
                    else -> callback.onOaidResult(code, "unknown error", null, null, null)
                }

                latch.await(15, TimeUnit.SECONDS)
                return result[0]

            } catch (t: Throwable) {
                t.printStackTrace()
                return CachedResult(199, "getOaid error " + t.message, null, null, null)
            }
        }

    }

    fun getOaid(context: Context, callback: OaidCallback) {
        //fetch方法内部也会判断，但此处直接判断可以减少不必要的方法调用和对象生成
        val cachedResult = cacheable.get()
        if (cachedResult != null) {
            callback.onOaidResult(cachedResult.code,
                cachedResult.msg,
                cachedResult.oaid,
                cachedResult.vaid,
                cachedResult.aaid)
            return
        }

        if (contextRef == null || contextRef?.get() == null) {
            contextRef = WeakReference(context)
        }
        cacheable.fetch(object : Cacheable.MainDataCallback<CachedResult>() {
            override fun onNext(e: CachedResult?) {
                e?.let {
                    callback.onOaidResult(it.code, it.msg, it.oaid, it.vaid, it.aaid)
                } ?: callback.onOaidResult(199, "unknown error")
            }

            override fun onError(t: Throwable?) {
                callback.onOaidResult(199, "getOaid error " + t?.message)
            }

        })
    }

    private var sOaidCertProvider: OaidCertProvider? = null
    fun setOaidCertProvider(provider: OaidCertProvider?) {
        sOaidCertProvider = provider
    }

    internal fun notifyUpdateCert(): Boolean {
        return sOaidCertProvider?.updateCert() ?: false
    }

    internal fun loadCert(): String? {
        return sOaidCertProvider?.loadCert()
    }

}

internal const val TAG = "OaidFacade"
internal fun log(msg: String) {
    Log.i(TAG, msg)
}

internal class IdentifierListenerHandler(var callback: OaidCallback) : InvocationHandler {
    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        //public void OnSupport(boolean isSupport, IdSupplier idSupplier)
        //void onSupport(IdSupplier paramIdSupplier)
        if ("OnSupport".equals(method.name, ignoreCase = true)) {
            try {
                val obj_idSupplier =
                    OaidFacade.IMPL.getIdSupplierOnSupport(args)
                if (obj_idSupplier == null) {
                    callback.onOaidResult(199, "idSupplier is null", null, null, null)
                    return null
                }
                val hostClassLoader = javaClass.classLoader
                val cls_IdSupplier =
                    hostClassLoader.loadClass("com.bun.miitmdid.interfaces.IdSupplier")
                val m_getOAID = cls_IdSupplier.getDeclaredMethod("getOAID")
                val m_getVAID = cls_IdSupplier.getDeclaredMethod("getVAID")
                val m_getAAID = cls_IdSupplier.getDeclaredMethod("getAAID")
                val oaid = m_getOAID.invoke(obj_idSupplier) as String
                val vaid = m_getVAID.invoke(obj_idSupplier) as String
                val aaid = m_getAAID.invoke(obj_idSupplier) as String
                log("invocation oaid=$oaid")
                callback.onOaidResult(200, "success", oaid, vaid, aaid)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        return null
    }
}
internal data class CachedResult(
    val code: Int,
    val msg: String?,
    val oaid: String? = null,
    val vaid: String? = null,
    val aaid: String? = null,
)

interface OaidCallback {
    fun onOaidResult(
        code: Int,
        msg: String?,
        oaid: String? = null,
        vaid: String? = null,
        aaid: String? = null,
    )
}

interface OaidCertProvider {
    /**
     * (建议异步)更新cert
     * @return 更新成功，需要重新initCert
     */
    fun updateCert(): Boolean

    /**
     * 自定义加载cert内容(如: 从服务端下载更新后的文件)
     *
     * @return
     */
    fun loadCert(): String?
}


