package com.jiashie.oaidfacade

import android.content.Context
import java.lang.Boolean
import kotlin.Any
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Throwable
import kotlin.let

internal class OaidSdkImplV1013 : OaidSdkBase(){
    companion object {

        @JvmStatic
        fun tryCreate(): OaidSdk? {
            try {
                val cls_MdidSdkHelper: Class<*> =
                    OaidSdkImplV1025::class.java.getClassLoader()
                        ?.loadClass(MdidSdkHelper_className) ?: return null
                //private String sdk_date = "20200702"
                val f_sdk_date = cls_MdidSdkHelper.getDeclaredField("sdk_date")
                f_sdk_date.isAccessible = true
                val obj_sdkHelper = cls_MdidSdkHelper.newInstance()
                val sdk_date = f_sdk_date[obj_sdkHelper] as String
                // 1.0.13的值2020011018
                log("oaid sdk vcode=$sdk_date")
                if ("2020011018" == sdk_date) {
                    return OaidSdkImplV1013()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            return null
        }
    }

    override fun getIdentifierListenerClassName(): String {
        return "com.bun.supplier.IIdentifierListener"
    }
    override fun initSdk(context: Context, handler: IdentifierListenerHandler): Int {
        try {
            val hostClassLoader = context.classLoader
            //com.bun.miitmdid.core.JLibrary.InitEntry(context)
            val cls_JLibrary = hostClassLoader.loadClass("com.bun.miitmdid.core.JLibrary")
            val m_InitEntry = cls_JLibrary.getDeclaredMethod("InitEntry", Context::class.java)
            m_InitEntry.invoke(null, context)
        } catch (t: Throwable) {
            t.printStackTrace()
            return ErrorCode.INIT_HELPER_CALL_ERROR
        }
        return super.initSdk(context, handler).let {
            //1.0.13返回0也是正确结果
            if (it == 0) {
                return@let ErrorCode.INIT_ERROR_RESULT_DELAY
            }
            it
        }
    }
    override fun doInitSdk(context: Context, objIIdentifierListener: Any): Int {
        try {
            val hostClassLoader = context.classLoader
            val cls_MdidSdkHelper = hostClassLoader.loadClass(MdidSdkHelper_className)
            val cls_IIdentifierListener =
                hostClassLoader.loadClass(getIdentifierListenerClassName())
            //assert cls_IIdentifierListener.isAssignableFrom(objIIdentifierListener.getClass());
            val m_InitSdk = cls_MdidSdkHelper.getDeclaredMethod("InitSdk",
                Context::class.java, Boolean.TYPE, cls_IIdentifierListener)
            return m_InitSdk.invoke(null, context, true, objIIdentifierListener) as Int
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return ErrorCode.INIT_HELPER_CALL_ERROR
    }

    override fun getIdSupplierOnSupport(args: Array<Any?>?): Any? {
        return if (args != null && args.size >= 2) {
            args[1]
        } else null
    }
}