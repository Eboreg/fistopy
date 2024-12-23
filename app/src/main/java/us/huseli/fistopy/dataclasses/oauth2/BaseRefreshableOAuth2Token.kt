package us.huseli.fistopy.dataclasses.oauth2

import com.google.gson.annotations.SerializedName

data class BaseRefreshableOAuth2Token(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    val scope: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("refresh_token")
    val refreshToken: String,
)
