package ru.skillbranch.kotlinexample

import java.util.*

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(fullName: String, email: String, password: String): User{
        return map[email.toLowerCase().trim()]?.let{
            throw IllegalArgumentException("A user with this email already exists")} ?:
        User.makeUser(fullName, email=email, password = password).also {user -> map[user.login] = user }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User{
        return map[getFormattedPhone(rawPhone)]?.also{
            throw IllegalArgumentException("A user with this phone already exists")} ?:
        User.makeUser(fullName, phone = rawPhone).also {user -> map[user.login] = user }
    }

    fun loginUser(login: String, password: String) : String? {
        return map[normalizeLogin(login)]?.run{
            if (checkPassword(password))
                this.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String) : Unit {
        map[normalizeLogin(login)]?.run {
            val newAccessCode = this.generateAccessCode()
            this.changePassword(accessCode!!, newAccessCode)
            this.accessCode = newAccessCode
        }
    }

    fun importUsers(list: List<String>): List<User> {
        val userList = arrayListOf<User>()
        list.forEach{
            val (fullName, email, access, phone) =
                it.split(";").map{it.trim().ifBlank { null }}.subList(0,4)
            userList.add(User.makeUser(fullName = fullName!!, email = email, phone = phone,
                password = access!!.substringAfter(":"), salt = access.substringBefore(":"))
                .also{map[it.login] = it})
        }
        return userList
    }

    private fun String.isLetterContained(): Boolean {
        this.forEach { if(it.isLetter()) return true }
        return false
    }

    fun clear() {
        map.clear()
    }

    private fun getFormattedPhone(rawPhone: String): String {
        val numberPhone =  "+".plus(rawPhone.replace("[^0-9]".toRegex(), ""))
        return if (rawPhone[0]!='+' || rawPhone.isLetterContained() || numberPhone.length != 12)
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        else numberPhone
    }

    private fun normalizeLogin(login: String):String {
        return if(login.length > 0 && login[0] == '+') getFormattedPhone(login)
        else login//.toLowerCase()
    }
}
