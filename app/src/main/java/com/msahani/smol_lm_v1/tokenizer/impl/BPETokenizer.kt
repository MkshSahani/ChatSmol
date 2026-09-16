package com.msahani.smol_lm_v1.tokenizer.impl

import android.content.Context
import android.util.Log
import com.msahani.smol_lm_v1.constants.logger.LoggerConstants
import com.msahani.smol_lm_v1.tokenizer.Tokenizer
import org.json.JSONObject
import java.util.stream.Gatherer

class BPETokenizer(private val context: Context, private val vocabJsonFileName: String) : Tokenizer {
    private var vocabMap: Map<String, Long>
    private var invVocabMap: MutableMap<Long, String>
    companion object {
        val TAG: String = LoggerConstants.SMOLLM_LOGGER_TAG.value + "::" + BPETokenizer.Companion::class.java.simpleName
    }

    init {
        this.vocabMap = this.loadVocab(context, vocabJsonFileName)
        this.invVocabMap = mutableMapOf()
        this.vocabMap.forEach { (token, id) ->
            this.invVocabMap[id as Long] = token
        }
        Log.i(TAG, "number of tokens :: ${this.vocabMap.size}, number of tokens in inv map :: ${this.invVocabMap.size}")
        this.logVocab()
    }

    override fun loadVocab(
        context: Context,
        vocabJsonPath: String
    ): Map<String, Long> {
        return this.loadVocabFromAssets(
            context,
            vocabJsonPath
        )
    }

    override fun getStringToken(tokenId: Long): String {
        TODO("Not yet implemented")
    }

    private fun loadVocabFromAssets(
        context: Context,
        vocabJsonPath: String
    ) : Map<String, Long> {
        val jsonText = context.assets.open(vocabJsonPath).bufferedReader().use { it.readText() }
        return JSONObject(jsonText).toMap() as Map<String, Long>
    }

    fun loadMergesFromAssets(mergeFileName: String) : List<Pair<String, String>> {
        return listOf<Pair<String, String>>();
    }

    private fun logVocab() {
        for((key, value) in this.vocabMap) {
            Log.i(TAG, "key :: $key value :: $value")
        }
    }
}

fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    keys().forEach { key ->
        map[key] = get(key)
    }
    return map
}

