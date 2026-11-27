package com.robbi.soracast.home

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.utils.AppContextUtils.loadResult

private tailrec fun Context?.findFragmentActivity(): FragmentActivity? = when (this) {
    null -> null
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

fun openSearchResponse(
    context: Context?,
    item: SearchResponse,
    startAction: Int = 0,
    startValue: Int = 0
) {
    context.findFragmentActivity()?.loadResult(
        url = item.url,
        apiName = item.apiName,
        name = item.name,
        startAction = startAction,
        startValue = startValue
    )
}
