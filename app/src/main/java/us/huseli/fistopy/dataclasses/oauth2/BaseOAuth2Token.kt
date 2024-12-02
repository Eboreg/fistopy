package us.huseli.fistopy.dataclasses.oauth2

import com.google.gson.annotations.SerializedName

data class BaseOAuth2Token(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
)
