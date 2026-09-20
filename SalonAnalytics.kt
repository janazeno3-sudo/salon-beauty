
package com.salonbeauty.app

data class CurrencyTotals(val syp: Double=0.0, val usd: Double=0.0, val eur: Double=0.0)

fun currencyTotal(sales: List<Sale>): CurrencyTotals {
    var s=0.0; var u=0.0; var e=0.0
    sales.forEach { when(it.currency) { "SYP"->s+=it.amount; "EUR"->e+=it.amount; else->u+=it.amount } }
    return CurrencyTotals(s,u,e)
}
