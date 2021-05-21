package com.hb.videoplayermanager

import java.io.Serializable

data class EncryptionConfig(val algorithm: String,
                            val key: String) :Serializable
