package com.msahani.smol_lm_v1.constants.model

sealed class SMOLLMConstants(val value: Int) {

    object NUM_LAYERS: SMOLLMConstants(30)
    object HEAD_DIM: SMOLLMConstants(64)
    object NUM_KV_HEAD: SMOLLMConstants(3)

}