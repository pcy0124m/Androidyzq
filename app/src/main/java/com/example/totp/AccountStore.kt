package com.example.totp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Account(val name: String, val secret: String)

class AccountStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("totp_accounts", Context.MODE_PRIVATE)

    fun load(): MutableList<Account> {
        val arr = JSONArray(prefs.getString("accounts", "[]") ?: "[]")
        val list = mutableListOf<Account>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Account(obj.getString("name"), obj.getString("secret")))
        }
        return list
    }

    fun add(name: String, secret: String) {
        val list = load()
        list.add(Account(name.trim(), secret.trim().replace(" ", "")))
        save(list)
    }

    fun delete(position: Int) {
        val list = load()
        if (position in list.indices) {
            list.removeAt(position)
            save(list)
        }
    }

    private fun save(list: List<Account>) {
        val arr = JSONArray()
        for (a in list) {
            arr.put(JSONObject().put("name", a.name).put("secret", a.secret))
        }
        prefs.edit().putString("accounts", arr.toString()).apply()
    }
}
