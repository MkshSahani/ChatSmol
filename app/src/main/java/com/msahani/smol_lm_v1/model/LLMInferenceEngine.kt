package com.msahani.smol_lm_v1.model

interface LLMInferenceEngine {

    fun generate(
        promptTokenIDS: LongArray,
        maxNumberOfTokens: Int,
        eosTokenID: Int
    ) : LongArray

    fun runInference(
        promptTokenIDS: LongArray,
        attentionMask: LongArray,
        positionIds: LongArray
    ) : Long
}