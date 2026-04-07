package com.example.myapplication.service.notification

object AppPackageMapping {
    val SUPPORTED_APPS = mapOf(
        "com.tencent.mm" to AppInfo("WeChat", "微信"),
        "com.whatsapp" to AppInfo("WhatsApp", "WhatsApp"),
        "org.telegram.messenger" to AppInfo("Telegram", "Telegram"),
        "com.tencent.mobileqq" to AppInfo("QQ", "QQ"),
        "com.skype.rover" to AppInfo("Skype", "Skype"),
        "com.viber.voip" to AppInfo("Viber", "Viber"),
        "com.linecorp.line" to AppInfo("LINE", "LINE"),
        "com.facebook.orca" to AppInfo("Facebook Messenger", "Messenger"),
        "com.facebook.messenger" to AppInfo("Facebook Messenger", "Messenger"),
        "jp.naver.linechat" to AppInfo("LINE", "LINE"),
        "com.snapchat" to AppInfo("Snapchat", "Snapchat"),
        "com.instagram.android" to AppInfo("Instagram", "Instagram"),
        "com.kakao.talk" to AppInfo("KakaoTalk", "KakaoTalk"),
        "com.twitter.android" to AppInfo("Twitter/X", "Twitter")
    )

    data class AppInfo(val packageName: String, val displayName: String)

    fun getDisplayName(packageName: String): String {
        return SUPPORTED_APPS[packageName]?.displayName ?: packageName
    }

    fun isSupported(packageName: String): Boolean {
        return SUPPORTED_APPS.containsKey(packageName)
    }
}
