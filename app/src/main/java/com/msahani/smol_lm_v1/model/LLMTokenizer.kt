package com.msahani.smol_lm_v1.model

interface LLMTokenizer {
    fun applyChatTemplate(prompt: String): String

    fun tokenize(prompt: String): LongArray

    fun decode(tokenId: Long): String

    fun getEOSToken(): Long
}