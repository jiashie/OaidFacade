package com.jiashie.oaidfacade

import android.content.Context

internal class OaidSdkImplStub : OaidSdk {

    override fun getIdSupplierOnSupport(args: Array<Any?>?): Any? = null

    override fun initSdk(context: Context, handler: IdentifierListenerHandler) = ErrorCode.INIT_HELPER_CALL_ERROR
}