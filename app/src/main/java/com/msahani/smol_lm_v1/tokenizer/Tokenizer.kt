package com.msahani.smol_lm_v1.tokenizer

import android.content.Context
import org.json.JSONObject

interface Tokenizer {
    fun loadVocab(context: Context, vocabJsonPath: String): Map<String, Long>
    fun getStringToken(tokenId: Long): String
}