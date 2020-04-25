package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        val us: User = User.makeUser(fullName, email = email, password = password)
        if (map.containsKey(us.login)) {
            throw IllegalArgumentException("A user with this email already exists")
        }

        return us.also { user ->
            map[user.login] = user
        }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User {
        val us: User = User.makeUser(fullName, phone = rawPhone)
        if (map.containsKey(us.login)) {
            throw IllegalArgumentException("A user with this phone already exists")
        }

        return us.also { user ->
            map[user.login] = user
        }
    }

    fun loginUser(login: String, password: String): String? {
        var log:String = login

        val plus :String? = login?.let {
            Regex(pattern = """\+""")
                .find(input = it)?.value
        }
        if (plus != null && plus.length == 1) {
            log = login.replace("[^+\\d]".toRegex(), "")
        }

        return map[log.trim()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String) : Unit {
        var log:String = login

        val plus :String? = login?.let {
            Regex(pattern = """\+""")
                .find(input = it)?.value
        }
        if (plus != null && plus.length == 1) {
            log = login.replace("[^+\\d]".toRegex(), "")
        }


        val user = map[log]
        user?:throw IllegalArgumentException("Unregistred phone number")
        user.requestAccessCode()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }

}