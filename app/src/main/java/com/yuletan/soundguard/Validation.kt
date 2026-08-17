package com.yuletan.soundguard

import android.util.Patterns
import com.google.i18n.phonenumbers.PhoneNumberUtil

object Validation {
    fun isEmailValid(value: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

    fun isPhoneValid(value: String): Boolean {
        val trimmed = value.trim()
        if (!trimmed.startsWith("+")) return false
        return runCatching {
            val util = PhoneNumberUtil.getInstance()
            val parsed = util.parse(trimmed, null)
            util.isValidNumber(parsed)
        }.getOrDefault(false)
    }
}
