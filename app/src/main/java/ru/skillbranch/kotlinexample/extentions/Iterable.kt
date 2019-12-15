package ru.skillbranch.kotlinexample.extentions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    return this.take(this.indexOfFirst(predicate))
}

