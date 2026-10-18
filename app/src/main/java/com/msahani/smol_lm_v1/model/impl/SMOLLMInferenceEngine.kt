package com.msahani.smol_lm_v1.model.impl

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.msahani.smol_lm_v1.constants.logger.LoggerConstants
import com.msahani.smol_lm_v1.model.LLMInferenceEngine
import com.msahani.smol_lm_v1.tokenizer.impl.BPETokenizer

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
    ): MutableList<String> {
        TODO("Not yet implemented")
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
            Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "Error while closing the ort sesssion. Error :: ${ex.message}")
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