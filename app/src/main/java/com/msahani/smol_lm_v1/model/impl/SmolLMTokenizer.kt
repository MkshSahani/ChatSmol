package com.msahani.smol_lm_v1.model.impl

import com.msahani.smol_lm_v1.model.LLMTokenizer


class SmolLMTokenizer(private val vocabJSONPath: String) : LLMTokenizer {


    override fun applyChatTemplate(prompt: String): String {
        return ""
    }

    override fun tokenize(prompt: String): LongArray {
        return LongArray(2)
    }

    override fun decode(tokenId: Long): String {
        return "NULL"
    }

    override fun getEOSToken(): Long {
        return -1L
    }

}