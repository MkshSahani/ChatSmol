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
    private lateinit var mergeRules: List<Pair<String, String>>
    private val pretokenizeRegex = Regex(
        "'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+"
    )
    companion object {
        val TAG: String = LoggerConstants.SMOLLM_LOGGER_TAG.value + BPETokenizer.Companion::class.java.simpleName
    }

    init {
        try {
            this.vocabMap = this.loadVocabFromAssets(vocabFileName)
            this.invVocabMap = this.buildInvVocabMap(this.vocabMap)
            this.mergeRules = this.loadMergesFromAssets(this.mergeFileName)
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "vocab size :: ${vocabMap.size}, inv vocab size :: ${invVocabMap.size}, merge rules length :: ${mergeRules.size}")
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error :: ${ex.message}")
        }
    }

    fun encode(text: String) {
        val tokens: List<String> = this.splitKeepingDelimiter(text, this.pretokenizeRegex)
        Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "tokens :: ${tokens}")
    }

    private fun splitKeepingDelimiter(text: String, delimiterRegEx: Regex) : List<String> {
        val result = mutableListOf<String>()
        var lastEnd = 0
        for(match in delimiterRegEx.findAll(text)) {
            if(match.range.first > lastEnd) {
                result.add(text.substring(lastEnd, match.range.first))
            }
            result.add(match.value)
            lastEnd = match.range.last + 1
        }
        return result
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


}


