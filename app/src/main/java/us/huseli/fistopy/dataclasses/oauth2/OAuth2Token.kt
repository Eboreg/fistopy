package us.huseli.fistopy.dataclasses.oauth2

import com.google.gson.Gson
import com.google.gson.GsonBuilder


open class OAuth2Token(val accessToken: String, val expires: Long) {
    fun expiresSoon() = expires < System.currentTimeMillis().plus(60 * 5)
    fun isExpired() = expires < System.currentTimeMillis()
    fun toJson(): String = gson.toJson(this)

    companion object {
        val gson: Gson = GsonBuilder().create()

        fun fromBaseJson(value: String): OAuth2Token {
            val base = gson.fromJson(value, BaseOAuth2Token::class.java)

            return OAuth2Token(
                accessToken = base.accessToken,
                expires = System.currentTimeMillis().plus(base.expiresIn * 1000),
            )
        }

        fun fromJson(value: String): OAuth2Token? {
            return try {
                gson.fromJson(value, OAuth2Token::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
