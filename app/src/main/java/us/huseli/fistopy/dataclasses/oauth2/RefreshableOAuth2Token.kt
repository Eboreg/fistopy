package us.huseli.fistopy.dataclasses.oauth2

class RefreshableOAuth2Token(
    accessToken: String,
    val scope: String,
    val refreshToken: String,
    expires: Long,
) : OAuth2Token(accessToken, expires) {
    companion object {
        fun fromBaseJson(value: String): RefreshableOAuth2Token {
            val base = gson.fromJson(value, BaseRefreshableOAuth2Token::class.java)

            return RefreshableOAuth2Token(
                accessToken = base.accessToken,
                scope = base.scope,
                refreshToken = base.refreshToken,
                expires = System.currentTimeMillis().plus(base.expiresIn * 1000),
            )
        }

        fun fromJson(value: String): RefreshableOAuth2Token? {
            return try {
                gson.fromJson(value, RefreshableOAuth2Token::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}