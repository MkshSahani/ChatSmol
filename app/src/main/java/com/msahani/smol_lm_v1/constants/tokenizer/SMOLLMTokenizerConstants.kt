package com.msahani.smol_lm_v1.constants.tokenizer

sealed class SMOLLMTokenizerConstants(val value: String) {
    object IM_START_TOKEN: SMOLLMTokenizerConstants("<|im_start|>")
    object IM_END_TOKEN: SMOLLMTokenizerConstants("<|im_end|>")
    object UNK_TOKEN: SMOLLMTokenizerConstants("<|endoftext|")
    object REPO_NAME_TOKEN: SMOLLMTokenizerConstants("<repo_name>")
    object REPONAME_TOKEN: SMOLLMTokenizerConstants("<reponame>")
    object FILENAME_TOKEN: SMOLLMTokenizerConstants("<filename>")
    object GH_STARS_TOKEN: SMOLLMTokenizerConstants("<gh_stars>")
    object ISSUE_START_TOKEN: SMOLLMTokenizerConstants("<issue_start>")
    object ISSUE_COMMENT_TOKEN : SMOLLMTokenizerConstants("<issue_comment>")
    object ISSUE_CLOSED_TOKEN : SMOLLMTokenizerConstants("<issue_closed>")
    object JUPYTER_START_TOKEN : SMOLLMTokenizerConstants("<jupyter_start>")
    object JUPYTER_TEXT_TOKEN : SMOLLMTokenizerConstants("<jupyter_text>")
    object JUPYTER_CODE_TOKEN : SMOLLMTokenizerConstants("<jupyter_code>")
    object JUPYTER_OUTPUT_TOKEN : SMOLLMTokenizerConstants("<jupyter_output>")
    object JUPYTER_SCRIPT_TOKEN : SMOLLMTokenizerConstants("<jupyter_script>")
    object EMPTY_OUTPUT_TOKEN : SMOLLMTokenizerConstants("<empty_output>")


}