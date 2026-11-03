package com.mahmoud.clipkid

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform