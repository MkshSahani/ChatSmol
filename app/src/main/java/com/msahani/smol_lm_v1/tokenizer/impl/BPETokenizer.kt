package com.msahani.smol_lm_v1.tokenizer.impl

import android.content.Context
import android.content.res.AssetManager
import android.nfc.Tag
import android.util.Log
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.msahani.smol_lm_v1.constants.logger.LoggerConstants
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.EMPTY_OUTPUT_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.FILENAME_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.GH_STARS_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.IM_END_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.IM_START_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.ISSUE_CLOSED_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.ISSUE_COMMENT_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.ISSUE_START_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.JUPYTER_CODE_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.JUPYTER_OUTPUT_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.JUPYTER_SCRIPT_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.JUPYTER_START_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.JUPYTER_TEXT_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.REPONAME_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.REPO_NAME_TOKEN
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants.UNK_TOKEN
import com.msahani.smol_lm_v1.tokenizer.Tokenizer
import org.jetbrains.annotations.Async
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import java.util.stream.Gatherer

class BPETokenizer(private val context: Context, private val vocabFileName: String, private val mergeFileName: String) : Tokenizer {

    private lateinit var vocab: HashMap<String, Long>
    private lateinit var invVocab: HashMap<Long, String>
    private lateinit var mergeRanks: HashMap<Pair<String, String>, Long>
    private lateinit var byteEncoder: HashMap<Int, Char>
    private lateinit var byteDecoder: HashMap<Char, Int>
    private val preTokenPattern = Pattern.compile(
        """'(?:[sdmt]|ll|ve|re)| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+[\r\n]*|\s*[\r\n]+|\s+(?!\S)|\s+"""
    )

    val SPECIAL_TOKEN_LIST = listOf<String>(
        UNK_TOKEN.value,
        IM_START_TOKEN.value,
        IM_END_TOKEN.value,
        REPO_NAME_TOKEN.value,
        REPONAME_TOKEN.value,
        FILENAME_TOKEN.value,
        GH_STARS_TOKEN.value,
        ISSUE_START_TOKEN.value,
        ISSUE_COMMENT_TOKEN.value,
        ISSUE_CLOSED_TOKEN.value,
        JUPYTER_START_TOKEN.value,
        JUPYTER_TEXT_TOKEN.value,
        JUPYTER_CODE_TOKEN.value,
        JUPYTER_OUTPUT_TOKEN.value,
        JUPYTER_SCRIPT_TOKEN.value,
        EMPTY_OUTPUT_TOKEN.value
    )
    private val specialTokenPattern = Pattern.compile(
        SPECIAL_TOKEN_LIST.sortedByDescending { it.length }.joinToString("|") { Pattern.quote(it) }
    )

//    private val byteEncoder: Map<Int, Char>
//    private val byteDecoder: Map<Char, Int>

    init {
        try {
            this.loadTokenizerAssets(context, vocabFileName, mergeFileName)
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while loading tokenizer assets.")
        }
    }

    fun encode(text: String, applyChatTemplate: Boolean) : LongArray {
        try {
            val result = ArrayList<Long>()
            val matchers = this.specialTokenPattern.matcher(text)
            var lastPosition = 0
            while(matchers.find()) {
                if(matchers.start() > lastPosition) {
                    encodeOrdinaryText(text.substring(lastPosition, matchers.start()), result)
                }
                val specialToken = matchers.group()
                val id: Long? = this.vocab[specialToken]
                if(id == null) {
                    Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Not found special token $specialToken in vocab")
                    throw Exception("Not Found $specialToken in vocab.")
                }
                result.add(id)
                lastPosition = matchers.end()
            }
            if(lastPosition < text.length) {
                encodeOrdinaryText(text.substring(lastPosition), result)
            }
            return result.toLongArray()
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while encoding text. Error :: ${ex.message}")
            throw ex
        }
    }

    private fun encodeOrdinaryText(
        text: String, result: MutableList<Long>
    ) : Unit {
        try {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "encoding text : $text")
            val matcher = preTokenPattern.matcher(text)
            while(matcher.find()) {
                val piece = matcher.group()
                Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "piece :: $piece")
                val encodedBytes = byteEncode(piece)
                Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "encoded bytes :: $encodedBytes")
                val symbols: MutableList<String> = applyBpe(encodedBytes)
                Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "After applying BPE symbols :: $symbols")
                for(symbol in symbols) {
                    if(this.vocab[symbol] != null) {
                        result.add(this.vocab[symbol]!!)
                    } else {
                        result.add(this.vocab[IM_END_TOKEN.value]!!)
                    }
                }
            }
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while encoding ordinary text :: $text. Error :: ${ex.message}")
            throw ex
        }
    }

    private fun applyBpe(token: String): MutableList<String> {
        try {
            if(token.length <= 1) {
                return mutableListOf(token)
            }
            val symbols = token.map { it.toString() }.toMutableList()
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "symbols $symbols")
            while(symbols.size > 1) {
                var bestIndex = -1
                var bestRank = Long.MAX_VALUE
                for(i in 0 until symbols.size - 1) {
                    val pair = Pair(symbols[i], symbols[i + 1])
                    val rank = mergeRanks[pair] ?: continue

                    if(rank < bestRank) {
                        bestIndex = i
                        bestRank = rank
                    }
                }
                if(bestIndex == -1)
                    break
                val merged = symbols[bestIndex] + symbols[bestIndex + 1]
                symbols[bestIndex] = merged
                symbols.removeAt(bestIndex + 1)
            }
            return symbols
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while applying BPE :: $token. Error :: ${ex.message}")
            throw ex
        }
    }

    private fun byteEncode(text: String): String {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val result = StringBuilder(bytes.size)
        for (byte in bytes) {
            val unsignedByte = byte.toInt() and 0xFF
            result.append(byteEncoder.getValue(unsignedByte))
        }
        return result.toString()
    }

    private fun loadTokenizerAssets(context: Context, vocabFileName: String, mergeFileName: String) {
        try {
            this.vocab = this.loadVocabFromAsset(context, vocabFileName)
            this.invVocab = this.buildInvVocab(this.vocab)
            this.mergeRanks = this.buildMergeRules(context, mergeFileName)
            var encoderDecoderPair = this.buildByteEncoderAndDecoder()
            this.byteEncoder = encoderDecoderPair.first
            this.byteDecoder = encoderDecoderPair.second
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "vocab size :: ${this.vocab.size} in vocab size :: ${this.invVocab.size}, merge rules count :: ${this.mergeRanks.size}")
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while loading tokenizer assets. Error :: ${ex.message}")
            throw Exception("Error while loading tokenizer assets ${ex.message}")
        }
    }
    private fun loadVocabFromAsset(context: Context, vocabFileName: String) : HashMap<String, Long> {
        try {
            val assetManager = context.applicationContext.assets
            val vocabJSONText = assetManager.open(vocabFileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val vocabJSON = JSONObject(vocabJSONText)
            val keys = vocabJSON.keys()
            val vocabMap = HashMap<String, Long>()
            while (keys.hasNext()) {
                val token = keys.next()
                val id = vocabJSON.getLong(token)
                vocabMap[token] = id
            }
            return vocabMap
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while loading vocab ${ex.message}")
            throw Exception("Error while loading vocab ${ex.message}")
        }
    }

    private fun buildInvVocab(vocabMap: HashMap<String, Long>) : HashMap<Long, String> {
        try {
            val invVocabMAP = HashMap<Long, String>()
            val keySet =  vocabMap.keys
            for(k in keySet) {
                invVocabMAP[vocabMap[k]!!] = k
            }
            return invVocabMAP
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while building inv vocab map :: ${ex.message}")
            throw Exception("Error while building inv vocab map :: ${ex.message}")
        }

    }

    private fun buildMergeRules(context: Context, mergeFileName: String): HashMap<Pair<String, String>, Long> {
        try {
            val mergeResulMap = HashMap<Pair<String, String>, Long>()
            val assetManager: AssetManager = context.applicationContext.assets
            val mergeText: String = assetManager.open(mergeFileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
            var rank: Long = 0
            for(line in mergeText.lineSequence()) {
                if(line.isBlank() || line.startsWith("#")) {
                    continue
                }
                val part = line.trim().split(Regex("\\s+"), limit =  2)
                if(part.size == 2) {
                    mergeResulMap[Pair(part[0], part[1])] = rank
                    rank += 1L
                }
            }
            return mergeResulMap
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while building merge rules ${ex.message}")
            throw Exception("Error whild build merge rules. Error :: ${ex.message}")
        }
    }

    private fun logSmolLMTokenizerAssets() {
        val tokenList = this.vocab.keys
        for(tkn in tokenList.toList().subList(0, 10)) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "token :: $tkn value :: ${this.vocab[tkn]}")
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "inv map idx :: ${this.vocab[tkn]} value :: ${this.invVocab[this.vocab[tkn]]}")
        }
        val mergeRuleList = this.mergeRanks.keys.toList().subList(0, 10)
        for(mergeRule in mergeRuleList) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "${mergeRule.first}, ${mergeRule.second}, rank :: ${this.mergeRanks[mergeRule]}")
        }
    }

    private fun buildByteEncoderAndDecoder(): Pair<HashMap<Int, Char>, HashMap<Char, Int>> {
        try {
            val encoder = HashMap<Int, Char>()
            val decoder = HashMap<Char, Int>()
            val visibleBytes = mutableListOf<Int>()
            for (b in 33..126) visibleBytes.add(b)
            for (b in 161..172) visibleBytes.add(b)
            for (b in 174..255) visibleBytes.add(b)
            val visibleSet = visibleBytes.toHashSet()
            var extraCodePoint = 256
            for (b in 0..255) {
                val codePoint = if (b in visibleSet) {
                    b
                } else {
                    extraCodePoint++
                    extraCodePoint - 1
                }
                val ch = codePoint.toChar()
                encoder[b] = ch
                decoder[ch] = b
            }
            return Pair<HashMap<Int, Char>, HashMap<Char, Int>>(encoder, decoder);
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while loading ")
            throw ex
        }
    }
}


