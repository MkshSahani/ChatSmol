package com.msahani.smol_lm_v1

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.msahani.smol_lm_v1.constants.logger.LoggerConstants
import com.msahani.smol_lm_v1.constants.model.ModelConstants
import com.msahani.smol_lm_v1.constants.tokenizer.SMOLLMTokenizerConstants
import com.msahani.smol_lm_v1.model.impl.SMOLLMInferenceEngine
import com.msahani.smol_lm_v1.tokenizer.Tokenizer
import com.msahani.smol_lm_v1.tokenizer.impl.BPETokenizer
import com.msahani.smol_lm_v1.ui.theme.SMOLLMv1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMOLLMv1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        applicationContext,
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(context: Context, name: String, modifier: Modifier = Modifier) {

    val bpeTokenizer: BPETokenizer = BPETokenizer(
        context,
        ModelConstants.MODEL_VOCAB_FILE_NAME.value,
        ModelConstants.MODEL_VOCAB_MERGE_FILE_NAME.value
    )
    val inputIds = bpeTokenizer.encode("<|im_start|>user\n who is donald trump ? <|im_end|>", false)
    Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "input ids :: ${inputIds}")
    for(inputId in inputIds) {
        Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "input id :: $inputId")
    }
    val decodedString = bpeTokenizer.decode(inputIds)
    Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "decode str :: $decodedString")
    val smolLMInferenceEngine: SMOLLMInferenceEngine = SMOLLMInferenceEngine(context, ModelConstants.MODEL_FILE_PATH.value)
    val generatedTokenIds = smolLMInferenceEngine.generate(
        inputIds,
        maxNumberOfTokens = 10,
        eosTokenID = 2
    )
    smolLMInferenceEngine.close()
    val generatedTextContent =  bpeTokenizer.decode(generatedTokenIds)
    Log.i(LoggerConstants.SMOLLM_LOGGER_TAG.value, "generated text :: ${generatedTextContent}")
    Text(text = "Smol-LM-Inference", modifier = modifier)

}
