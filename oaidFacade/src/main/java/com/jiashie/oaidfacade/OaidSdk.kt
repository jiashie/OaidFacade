package com.jiashie.oaidfacade

import android.content.Context
import java.lang.reflect.Proxy

/**
 * 不同版本的MdidSdkHelper.initSdk方法参数不一样
 * IIdentifierListener.onSupport回调方法的参数也不一样
 */
internal interface OaidSdk {

    fun initSdk(context: Context, handler: IdentifierListenerHandler): Int

    /**
     * onSupport回调时，从参数获取IdSupplier实例
     * @param args 不同版本参数不同
     * @return com.bun.miitmdid.interfaces.IdSupplier的实例
     */
    fun getIdSupplierOnSupport(args: Array<Any?>?): Any?
}

class ErrorCode {
    companion object {
        const val INIT_ERROR_BEGIN = 1008610
        const val INIT_ERROR_MANUFACTURER_NOSUPPORT = 1008611
        const val INIT_ERROR_DEVICE_NOSUPPORT = 1008612
        const val INIT_ERROR_LOAD_CONFIGFILE = 1008613
        const val INIT_ERROR_RESULT_DELAY = 1008614
        const val INIT_HELPER_CALL_ERROR = 1008615
        const val INIT_ERROR_CERT_ERROR = 1008616
    }
}

internal abstract class OaidSdkBase : OaidSdk {
    companion object {
        const val MdidSdkHelper_className = "com.bun.miitmdid.core.MdidSdkHelper"
    }
    /**
     * 初始化sdk
     * @param context
     * @param objIIdentifierListener IIdentifierListener的实例(代理对象)
     * @return
     */
    protected abstract fun doInitSdk(context: Context, objIIdentifierListener: Any): Int

    protected open fun getIdentifierListenerClassName(): String {
        //1.0.13以上(不含)都是此类名
        return "com.bun.miitmdid.interfaces.IIdentifierListener"
    }

    override fun initSdk(context: Context, handler: IdentifierListenerHandler): Int {
        return try {
            val hostClassLoader = context.classLoader
            val cls_IIdentifierListener =
                hostClassLoader.loadClass(getIdentifierListenerClassName())
            val obj_identifierListener =
                Proxy.newProxyInstance(hostClassLoader,
                    arrayOf(cls_IIdentifierListener),
                    handler)
            doInitSdk(context, obj_identifierListener)
        } catch (t: Throwable){
            t.printStackTrace()
            ErrorCode.INIT_HELPER_CALL_ERROR
        }

    }


}