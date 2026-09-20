
package com.salonbeauty.app

import android.content.Context

class AuthManager(context: Context) {
    private val p = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    fun isLoggedIn() = p.getBoolean("logged_in", false)
    fun login(user: String, pass: String): Boolean {
        val ok = user == "admin" && pass == "1234"
        if (ok) p.edit().putBoolean("logged_in", true).apply()
        return ok
    }
    fun logout() { p.edit().putBoolean("logged_in", false).apply() }
}
