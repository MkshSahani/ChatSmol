package com.msahani.smol_lm_v1.constants.logger

sealed class LoggerConstants(val value: String) {

    object SMOLLM_LOGGER_TAG: LoggerConstants("SMOL-LM")

}