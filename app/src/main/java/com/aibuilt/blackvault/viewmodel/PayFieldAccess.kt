=== ABOUT: Small helper so the pay screen can read the typed text straight from its brain ===
package com.aibuilt.blackvault.viewmodel

fun PayViewModel.rawAccount(): String = fieldsSnapshot().account
fun PayViewModel.rawIfsc(): String = fieldsSnapshot().ifsc
fun PayViewModel.rawIban(): String = fieldsSnapshot().iban
fun PayViewModel.rawAba(): String = fieldsSnapshot().aba
