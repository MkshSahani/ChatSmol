package com.msahani.smol_lm_v1

import android.content.Context
import android.os.Bundle
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
import com.msahani.smol_lm_v1.constants.model.ModelConstants
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

    val bpeTokenizer: BPETokenizer = BPETokenizer(context, ModelConstants.MODEL_VOCAB_FILE_NAME.value)
    Text(text = "Smol-LM-Inference", modifier = modifier)

}
