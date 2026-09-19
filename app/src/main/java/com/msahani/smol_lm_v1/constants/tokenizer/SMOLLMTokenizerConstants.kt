package com.msahani.smol_lm_v1.constants.tokenizer

sealed class SMOLLMTokenizerConstants(val value: String) {
    object IM_START_TOKEN: SMOLLMTokenizerConstants("<|im_start|>")
    object IM_END_TOKEN: SMOLLMTokenizerConstants("<|im_end|>")
    object UNK_TOKEN: SMOLLMTokenizerConstants("<|endoftext|")

    object BOS_TOKEN: SMOLLMTokenizerConstants(IM_START_TOKEN.value)
    object EOS_TOKEN: SMOLLMTokenizerConstants(IM_END_TOKEN.value)
    object PAD_TOKEN: SMOLLMTokenizerConstants(IM_END_TOKEN.value)
}