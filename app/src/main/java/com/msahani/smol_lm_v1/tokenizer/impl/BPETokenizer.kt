package com.msahani.smol_lm_v1.tokenizer.impl

import android.content.Context
import android.util.Log
import com.msahani.smol_lm_v1.constants.logger.LoggerConstants
import com.msahani.smol_lm_v1.tokenizer.Tokenizer
import org.json.JSONObject
import java.util.stream.Gatherer

class BPETokenizer(private val context: Context, private val vocabFileName: String, private val mergeFileName: String) : Tokenizer {


    private lateinit var vocabMap: HashMap<String, Long>
    private lateinit var invVocabMap: HashMap<Long, String>

    companion object {
        val TAG: String = LoggerConstants.SMOLLM_LOGGER_TAG.value + BPETokenizer.Companion::class.java.simpleName
    }

    private fun buildInvVocabMap(vocabMap: HashMap<String, Long>): HashMap<Long, String> {
        val invVocabMapTemp: HashMap<Long, String> = HashMap<Long, String>()
        for(key in vocabMap.keys) {
            invVocabMapTemp[vocabMap[key] as Long] = key
        }
        return invVocabMapTemp
    }

    private fun loadVocabFromAssets(
        vocabJSONFilePath: String
    ): HashMap<String, Long> {
        try {
            val json = JSONObject(this.context.assets.open(vocabJSONFilePath).bufferedReader().use { it.readText() })
            val map = HashMap<String, Long>(json.length());
            for(key in json.keys()) {
                map[key] = json.getLong(key)
            }
            return map
        } catch (ex: Exception) {
            throw ex
        }
    }

    private fun loadMergesFromAssets(
        mergeFilePath: String
    ) : List<Pair<String, String>> {
        try {
            val mergeFileTxtContent = this.context.assets.open(mergeFilePath).bufferedReader().use { it.readLines() }
            return mergeFileTxtContent.filter {
                it.isNotBlank() && !it.startsWith("#")
            } .map(fun(line: String): Pair<String, String> {
                val parts = line.trim().split(Regex("\\s+"))
                require(parts.size == 2) { "Malformed merge line: '$line'" }
                return parts[0] to parts[1]
            })
        } catch (ex: Exception) {
            throw ex
        }
    }

    private fun bpe(token: String): List<String> {
        val symbols = token.map { it.toString() }.toMutableList()

    }

}


