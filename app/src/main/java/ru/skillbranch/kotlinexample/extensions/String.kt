package ru.skillbranch.kotlinexample.extensions

import ru.skillbranch.kotlinexample.Constants


fun String.getEmptyAsNull(): String? {
    return if(trim().isNullOrEmpty())
        null
    else
        this
}

fun String.getAsPhoneNumber():String? {
    return getEmptyAsNull()?.replace("[^+\\d]".toRegex(), "")
}

fun String.isPhoneNumber():Boolean {
    val test = getAsPhoneNumber()
    return  if (test == null)
                false
            else
                test?.contains('+') && test.length -1 == Constants.PHONE_LENGTH
}
