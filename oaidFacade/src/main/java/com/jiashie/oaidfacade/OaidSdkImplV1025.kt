package com.jiashie.oaidfacade

import android.content.Context
import android.text.TextUtils
import java.lang.Boolean
import kotlin.Any
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Throwable

/**
 * v1.0.25及之前版本
 */
internal class OaidSdkImplV1025 : OaidSdkBase(){
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
                //fixme 用于网游sdk时，游戏用的oaid版本可能不兼容。
                // 按1025的调用方式能兼容到哪个旧版sdk，需要逐个调研
                // 当前认为只要有这个字段值，就兼容
                // 1.0.13的值2020011018
                log("oaid sdk vcode=$sdk_date")
                if (!TextUtils.isEmpty(sdk_date) && "2020011018" != sdk_date) {
                    return OaidSdkImplV1025()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            return null
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