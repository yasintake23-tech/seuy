package com.example.config

object R2Config {
    // Cloudflare R2 Yapılandırması
    var accountId: String = "43b4ca86d044289213dea0875297561c"
    var bucketName: String = "ikimiz-media"
    var accessKeyId: String = "9a68a59f3ca7646af5acb5c5d388c733"
    var secretAccessKey: String = "28470eae688661f474134925ec38e0fef0090f3e00eb049a637417c484e5b7b9"
    var publicDomain: String = "https://43b4ca86d044289213dea0875297561c.r2.cloudflarestorage.com/ikimiz-media"

    val endpointUrl: String
        get() = if (accountId.isNotBlank()) "https://$accountId.r2.cloudflarestorage.com" else ""

    val isConfigured: Boolean
        get() = accountId.isNotBlank() && accessKeyId.isNotBlank() && secretAccessKey.isNotBlank() && bucketName.isNotBlank()
}
