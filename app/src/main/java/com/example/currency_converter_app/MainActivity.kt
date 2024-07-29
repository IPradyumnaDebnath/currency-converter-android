package com.example.currency_converter_app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.google.gson.Gson
import java.io.InputStream
import kotlin.time.times

class MainActivity : AppCompatActivity() {

    private val gson = Gson()
    private var isProgrammaticUpdate = false
    private var lastRequestTime: Long = 0
    private val debounceDelay: Long = 100 // 300 ms debounce
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var exchangeRatesResponse: ExchangeRatesResponse
    private lateinit var allCurrencies:MutableList<String>

    private lateinit var currencyFromSpinnerAdapter: ArrayAdapter<String>
    private lateinit var currencyFromSpinner: Spinner
    private lateinit var amountFrom: EditText


    private lateinit var currencyToSpinnerAdapter: ArrayAdapter<String>
    private lateinit var currencyToSpinner: Spinner
    private lateinit var amountTo: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        exchangeRatesResponse = readLocalJson("assets/exchange_rates.json")
        allCurrencies = (exchangeRatesResponse?.rates?.keys?.sorted() ?: emptySet()).toMutableList()
//Currency from Section
        currencyFromSpinner = findViewById(R.id.converted_from)
        amountFrom = findViewById(R.id.amount_from)
        currencyFromSpinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            allCurrencies
        )
        currencyFromSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencyFromSpinner.adapter = currencyFromSpinnerAdapter
        setDefaultSelection(currencyFromSpinner, "INR")
        amountFrom.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isProgrammaticUpdate) debounceUpdate(false)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        currencyFromSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                debounceUpdate(false)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        //Currency To Section
        currencyToSpinner = findViewById(R.id.converted_to)
        amountTo = findViewById(R.id.amount_to)
        currencyToSpinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            allCurrencies
        )
        currencyToSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencyToSpinner.adapter = currencyToSpinnerAdapter
        setDefaultSelection(currencyToSpinner, "USD")
        amountTo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isProgrammaticUpdate) debounceUpdate(true)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        currencyToSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                debounceUpdate(true)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

    }

    private fun setDefaultSelection(spinner: Spinner, defaultValue: String) {
        val adapter = spinner.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(defaultValue)
        if (position >= 0) {
            spinner.setSelection(position)
        }
    }

    private fun debounceUpdate(reverseUpdate: Boolean) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRequestTime >= debounceDelay) {
            lastRequestTime = currentTime
            performConversion(reverseUpdate)
        } else {
            handler.postDelayed({
                if (System.currentTimeMillis() - lastRequestTime >= debounceDelay) {
                    performConversion(reverseUpdate)
                }
            }, debounceDelay)
        }
    }

    private fun performConversion(reverseUpdate: Boolean) {
        val currentFrom = if (reverseUpdate) amountTo else amountFrom
        val currentTo = if (reverseUpdate) amountFrom else amountTo
        val currentFormSpinner = if (reverseUpdate) currencyToSpinner else currencyFromSpinner
        val currentToSpinner = if (reverseUpdate) currencyFromSpinner else currencyToSpinner

        val amountText = currentFrom.text.toString()
        if (amountText.isNotEmpty()) {
            val amount = amountText.toDouble()
            val fromCurrency = currentFormSpinner.selectedItem.toString().split(" - ")[0]
            val toCurrency = currentToSpinner.selectedItem.toString().split(" - ")[0]
            fetchExchangeRates(fromCurrency, toCurrency, amount, currentTo)
        }
    }

    private fun fetchExchangeRates(
        fromCurrency: String, toCurrency: String, amount: Double, textFieldToUpdate: EditText
    ) {

        Log.d("test exchangeRa", exchangeRatesResponse.toString())
        val defaultNumberValue = 1
        val fromRate: Double =
            exchangeRatesResponse.rates[fromCurrency] ?: defaultNumberValue.toDouble()
        val toRate: Double =
            exchangeRatesResponse.rates[toCurrency] ?: defaultNumberValue.toDouble()
        val result = (amount / fromRate) * toRate
        isProgrammaticUpdate = true
        textFieldToUpdate.setText(String.format("%.2f", result))
        isProgrammaticUpdate = false


    }

    private fun readLocalJson(resourceName: String): ExchangeRatesResponse {
        return try {
            val inputStream: InputStream =
                object {}.javaClass.getResourceAsStream("/$resourceName")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            gson.fromJson(jsonString, ExchangeRatesResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            ExchangeRatesResponse()

        }
    }


}