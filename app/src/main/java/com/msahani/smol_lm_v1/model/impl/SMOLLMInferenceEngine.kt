package com.msahani.smol_lm_v1.model.impl

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.util.Log
import com.msahani.smol_lm_v1.constants.logger.LoggerConstants
import com.msahani.smol_lm_v1.constants.model.ModelConstants
import com.msahani.smol_lm_v1.constants.model.ONNXKeyConstants
import com.msahani.smol_lm_v1.constants.model.SMOLLMConstants
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants
import com.msahani.smol_lm_v1.model.LLMInferenceEngine
import com.msahani.smol_lm_v1.tokenizer.impl.BPETokenizer
import kotlinx.coroutines.newSingleThreadContext
import java.nio.FloatBuffer
import java.nio.LongBuffer

class SMOLLMInferenceEngine(val context: Context, val onnxModelPath: String) : LLMInferenceEngine {

    private val smolLMONNXEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val smolLMONNXSession: OrtSession

    init {
        try {
            this.smolLMONNXSession = this.openOrtSession(context, onnxModelPath)

        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while creating smol lm inference engine instance. Error :: ${ex.message}")
            throw Exception("Error while creating smol lm inference engine instance. Error :: ${ex.message}")
        }
    }

    override fun generate(
        promptTokenIDS: LongArray,
        maxNumberOfTokens: Int,
        eosTokenID: Int
    ): LongArray {
        require(promptTokenIDS.isNotEmpty())
        require(maxNumberOfTokens >= 0)

        if(maxNumberOfTokens == 0) {
            return longArrayOf()
        }
        val generated = ArrayList<Long>()
        var cache: List<OnnxTensor>? = null
        var pastLength = 0
        try {
            var (nextToken, newCache) = infer(
                inputTokenIds = promptTokenIDS,
                pastKeyValue = cache,
                pastSequenceLength = pastLength
            )
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "next token :: ${nextToken.toInt()}")
            cache = newCache
            pastLength = promptTokenIDS.size
            repeat(maxNumberOfTokens) { step ->
                generated.add(nextToken)
                if(step == maxNumberOfTokens -  1) {
                    return generated.toLongArray()
                }
                val (predictedNextToken, updatedCache) = infer(
                    inputTokenIds = longArrayOf(nextToken),
                    pastKeyValue = cache,
                    pastSequenceLength = pastLength
                )
                Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "next token :: ${predictedNextToken.toInt()}")
                cache?.forEach {
                    it.close()
                }
                cache = updatedCache
                pastLength++
                nextToken = predictedNextToken
            }
            return generated.toLongArray()
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error :: ${ex.message}")
            throw ex
        }
    }

    fun infer(
        inputTokenIds: LongArray,
        pastKeyValue: List<OnnxTensor>?,
        pastSequenceLength: Int
    ) : Pair<Long, List<OnnxTensor>> {
        require(inputTokenIds.isNotEmpty())
        val inputs: LinkedHashMap<String, OnnxTensor> = LinkedHashMap<String, OnnxTensor>()
        val resultCache = mutableListOf<OnnxTensor>()
        try {
            val currentLength = inputTokenIds.size
            val totalLength = pastSequenceLength + inputTokenIds.size
            inputs[ONNXKeyConstants.KEY_ONNX_SMOL_LM_INPUT_IDS.value] = OnnxTensor.createTensor(
                smolLMONNXEnvironment,
                LongBuffer.wrap(inputTokenIds),
                longArrayOf(1L, currentLength.toLong())
            )
            inputs[ONNXKeyConstants.KEY_ONNX_SMOL_LM_ATTENTION_MASK.value] = OnnxTensor.createTensor(
                smolLMONNXEnvironment,
                LongBuffer.wrap(LongArray(totalLength) { 1L }),
                longArrayOf(1L, totalLength.toLong())
            )
            inputs[ONNXKeyConstants.KEY_ONNX_SMOL_LM_POSITION_IDS.value] = OnnxTensor.createTensor(
                smolLMONNXEnvironment,
                LongBuffer.wrap(LongArray(currentLength) {
                    index -> (currentLength + index).toLong()
                }),
                longArrayOf(1L, currentLength.toLong())
            )
            if(pastKeyValue == null) {
                for(layer in 0 until SMOLLMConstants.NUM_LAYERS.value) {
                    for(kind in listOf("key", "value")) {
                        val featureName = "past_${kind}_$layer"
                        val empty = FloatArray(0)
                        inputs[featureName] = OnnxTensor.createTensor(
                            smolLMONNXEnvironment,
                            FloatBuffer.wrap(empty),
                            longArrayOf(
                                1L,
                                SMOLLMConstants.NUM_KV_HEAD.value.toLong(),
                                0L,
                                SMOLLMConstants.HEAD_DIM.value.toLong()
                            )
                        )
                    }
                }
            } else {
                require(pastKeyValue.size == SMOLLMConstants.NUM_LAYERS.value * 2) {
                    Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "past key size should be ${2 * SMOLLMConstants.NUM_LAYERS.value}, but found ${pastKeyValue.size}")
                }
                pastKeyValue.forEachIndexed { index, tensor ->
                        val layer = index / 2
                        val kind = if (index % 2 == 0) "key" else "value"
                        inputs["past_${kind}_${layer}"] = tensor
                }
            }
            smolLMONNXSession.run(inputs).use { result ->
                val logitTensor = result[0] as OnnxTensor
                val logitInfo = logitTensor.info as TensorInfo
                val logitShape = logitInfo.shape
                val logitInfoShapeString = logitShape.contentToString()
                Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "logit info shape :: $logitInfoShapeString")
                val logit = logitTensor.floatBuffer
                val vocabSize = logitShape[2].toInt()
                val lastPositionOffset = (currentLength - 1) * vocabSize
                var bestToken = 0
                var bestLogit = Float.NEGATIVE_INFINITY
                for(i in 0 until vocabSize) {
                    val value = logit.get(lastPositionOffset + i)
                    if(value > bestLogit) {
                        bestLogit = value
                        bestToken = i
                    }
                }
                for(i in 1 until result.size()) {
                    val outputTensor = result[i] as OnnxTensor
                    val outputTensorInfo = outputTensor.info as TensorInfo
                    require(outputTensorInfo.shape.size == 4) {
                        "Expected rank-4 KV cache, got ${outputTensorInfo.shape.size}"
                    }
                    val elementCount = outputTensorInfo.shape.fold(1L) { acc, dim ->
                        acc*dim
                    }
                    require(elementCount <= Int.MAX_VALUE) {
                        "KV-cache tensor is too large to copy"
                    }
                    val source = outputTensor.floatBuffer
                    val copy = FloatArray(elementCount.toInt())
                    source.get(copy)
                    resultCache.add(
                        OnnxTensor.createTensor(
                            smolLMONNXEnvironment,
                            FloatBuffer.wrap(copy),
                            outputTensorInfo.shape
                        )
                    )
                }
                return bestToken.toLong() to resultCache
            }
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error : ${ex.message}")
            throw ex
        }
    }

    override fun runInference(
        promptTokenIDS: LongArray,
        attentionMask: LongArray,
        positionIds: LongArray
    ): Long {
        TODO("Not yet implemented")
    }

    public fun close() : Boolean {
        try {
            this.closeOrtSession()
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Close the ONNX Session.")
            return true
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while closing the ort session. Error :: ${ex.message}")
            return false
        }
    }

    private fun openOrtSession(context: Context, onnxModelPath: String) : OrtSession  {
        try {
            val onnxModelBytes = context.applicationContext.assets.open(onnxModelPath).use { it.readBytes() }
            val smolLMONNXSession = smolLMONNXEnvironment.createSession(
                onnxModelBytes,
                OrtSession.SessionOptions()
            )
            return smolLMONNXSession
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while opening ort session. Error :: ${ex.message}")
            throw Exception("Error while opening ort session. Error :: ${ex.message}")
        }
    }

    private fun closeOrtSession(): Boolean {
        try {
            if(this.smolLMONNXSession != null) {
                this.smolLMONNXSession.close();
            }
            if(this.smolLMONNXEnvironment != null) {
                this.smolLMONNXEnvironment.close()
            }
            return true
        } catch (ex: Exception) {
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while closing onnx session. Error ${ex.message}")
            throw ex
        }
    }


}