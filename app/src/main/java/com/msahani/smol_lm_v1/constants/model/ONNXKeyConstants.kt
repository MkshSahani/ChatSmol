package com.msahani.smol_lm_v1.constants.model

sealed class ONNXKeyConstants(val value: String) {

    object KEY_ONNX_SMOL_LM_INPUT_IDS : ONNXKeyConstants("input_ids")
    object KEY_ONNX_SMOL_LM_ATTENTION_MASK: ONNXKeyConstants("attention_mask")
    object KEY_ONNX_SMOL_LM_POSITION_IDS: ONNXKeyConstants("position_ids")
    object KEY_ONNX_SMOL_LM_PAST_KEY: ONNXKeyConstants("past_key_{}")
    object KEY_ONNX_SMOL_LM_PAST_VALUE: ONNXKeyConstants("past_value_{}")
    object KEY_ONNX_SMOL_LM_LOGITS: ONNXKeyConstants("logits")
    object KEY_ONNX_SMOL_LM_PRESENT_KEY: ONNXKeyConstants("present_key_{}")
    object KEY_ONNX_SMOL_LM_PRESENT_VALUE: ONNXKeyConstants("present_value_{}")

}