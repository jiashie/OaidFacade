package com.jiashie.oaidfacade

import android.content.Context
import android.text.TextUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * 需要证书关联包名
 * 从1.0.26开始
 */
internal class OaidSdkImplV1026 : OaidSdk {
    companion object {
        init {
            try {
                //从1.0.30开始才有此so
                System.loadLibrary("msaoaidsec")
            } catch (t: Throwable) {
            }
        }

        fun tryCreate(): OaidSdk? {
            try {
                val cls_MdidSdkHelper: Class<*> =
                    OaidSdkImplV1026::class.java.getClassLoader()
                        ?.loadClass("com.bun.miitmdid.core.MdidSdkHelper") ?: return null
                //public static final int SDK_VERSION_CODE
                val f_SDK_VERSION_CODE = cls_MdidSdkHelper.getDeclaredField("SDK_VERSION_CODE")
                f_SDK_VERSION_CODE.isAccessible = true
                val code = f_SDK_VERSION_CODE[null] as Int
                log("oaid sdk vcode=$code")
                if (code > 0) {
                    return OaidSdkImplV1026()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            return null
        }
    }

    private var isCertInit = false

    override fun initSdk(context: Context, objIIdentifierListener: Any): Int {
        try {
            initCert(context)
            val hostClassLoader = context.classLoader
            val cls_MdidSdkHelper = hostClassLoader.loadClass("com.bun.miitmdid.core.MdidSdkHelper")
            val cls_IIdentifierListener =
                hostClassLoader.loadClass("com.bun.miitmdid.interfaces.IIdentifierListener")
            //assert cls_IIdentifierListener.isAssignableFrom(objIIdentifierListener.getClass());
            var code = invokeInitSdkV1200(context,
                objIIdentifierListener,
                cls_MdidSdkHelper,
                cls_IIdentifierListener)
            if (code == -1) {
                val m_InitSdk = cls_MdidSdkHelper.getDeclaredMethod("InitSdk",
                    Context::class.java, java.lang.Boolean.TYPE, cls_IIdentifierListener)
                code = m_InitSdk.invoke(null, context, true, objIIdentifierListener) as Int
            }
            if (code == ErrorCode.INIT_ERROR_CERT_ERROR) {
                if (OaidFacade.notifyUpdateCert()) {
                    //重置，返回cert_error，facade不缓存结果，需要再次initSdk
                    // （可能异步执行，不能马上再次initSdk，由调用方自己在updateCert后再次getOaid)
                    isCertInit = false
                } else {
                    //证书错误，但又不更新，返回call_error
                    code = ErrorCode.INIT_HELPER_CALL_ERROR
                }
            }
            return code
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return ErrorCode.INIT_HELPER_CALL_ERROR
    }

    /**
     * 从1200开始提供了另一个initSdk方法，传入参数控制id是否可获取
     * @param cls_MdidSdkHelper
     * @param cls_IIdentifierListener
     * @return
     */
    private fun invokeInitSdkV1200(
        context: Context,
        objIIdentifierListener: Any,
        cls_MdidSdkHelper: Class<*>,
        cls_IIdentifierListener: Class<*>,
    ): Int {
        try {
            val m_InitSdk =
                cls_MdidSdkHelper.getDeclaredMethod("InitSdk",
                    Context::class.java,
                    java.lang.Boolean.TYPE,
                    java.lang.Boolean.TYPE,
                    java.lang.Boolean.TYPE,
                    java.lang.Boolean.TYPE,
                    cls_IIdentifierListener)
            //public static int InitSdk(Context paramContext, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, boolean paramBoolean4,
            // IIdentifierListener paramIIdentifierListener)
            //vaid、aaid暂不需要
            return m_InitSdk.invoke(null,
                context,
                true,
                true,
                false,
                false,
                objIIdentifierListener) as Int
        } catch (t: Throwable) {
        }
        return -1
    }

    private fun initCert(context: Context) {
        if (!isCertInit) {
            try {
                var cert: String? = OaidFacade.loadCert()
                if (TextUtils.isEmpty(cert)) {
                    cert = loadPemFromAssetFile(context, context.packageName + ".cert.pem")
                }
                if (TextUtils.isEmpty(cert)) {
                    return
                }
                val hostClassLoader = context.classLoader
                val cls_MdidSdkHelper =
                    hostClassLoader.loadClass("com.bun.miitmdid.core.MdidSdkHelper")
                val m_initCert = cls_MdidSdkHelper.getDeclaredMethod("InitCert",
                    Context::class.java,
                    java.lang.String::class.java)
                isCertInit = m_initCert.invoke(null, context, cert) as Boolean
                log("initCert=$isCertInit")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    override fun getIdSupplierOnSupport(args: Array<Any?>?): Any? {
        return if (args != null && args.isNotEmpty()) {
            args[0]
        } else null
    }

    /**
     * 从asset文件读取证书内容
     * @param context
     * @param assetFileName
     * @return 证书字符串
     */
    private fun loadPemFromAssetFile(context: Context, assetFileName: String): String? {
        return try {
            val inputStream = context.assets.open(assetFileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val builder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.append(line)
                builder.append('\n')
            }
            builder.toString()
        } catch (e: IOException) {
            log("loadPemFromAssetFile failed")
            ""
        }
    }
}