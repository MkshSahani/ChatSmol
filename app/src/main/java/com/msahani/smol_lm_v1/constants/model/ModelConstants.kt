package com.msahani.smol_lm_v1.constants.model

sealed class ModelConstants(val value: String) {
    object MODEL_FILE_PATH: ModelConstants("smol-lm-135M-int8.onnx")
    object MODEL_VOCAB_FILE_NAME: ModelConstants("vocab.json")
    object MODEL_VOCAB_MERGE_FILE_NAME: ModelConstants("merges.txt")
}