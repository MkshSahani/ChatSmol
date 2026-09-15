package com.msahani.smol_lm_v1.constants.model

sealed class ModelConstants(val value: String) {
    object MODEL_FILE_PATH: ModelConstants("smol-lm-135M0-fp32.onnnx")
    object MODEL_VOCAB_FILE_NAME: ModelConstants("vocab.json")
}