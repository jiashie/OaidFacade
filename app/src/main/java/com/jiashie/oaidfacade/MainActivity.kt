package com.jiashie.oaidfacade

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jiashie.oaidfacade.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOaid.setOnClickListener {
            binding.txtInfo.text = ""
            OaidFacade.getOaid(this, object : OaidCallback {
                override fun onOaidResult(
                    code: Int,
                    msg: String?,
                    oaid: String?,
                    vaid: String?,
                    aaid: String?,
                ) {
                    if (code == 200) {
                        binding.txtInfo.text = "oaid=${oaid}"
                    } else {
                        binding.txtInfo.text = "code=$code msg=$msg"
                    }
                }

            })
        }

    }
}