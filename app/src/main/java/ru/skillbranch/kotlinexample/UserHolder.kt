package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.getAsPhoneNumber
import ru.skillbranch.kotlinexample.extensions.isPhoneNumber

object UserHolder {
    private val map = mutableMapOf<String, User>()


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName,email= email,password = password)
            .also { user ->
                checkUserAndSave(user)
            }

    }

    fun registerUserByPhone(fullName: String, rawPhone: String):User {
        return User.makeUser(fullName,phone = rawPhone)
            .also {user ->
                checkUserAndSave(user, true)
            }
    }

    fun checkUserAndSave(user: User, byPhone: Boolean = false) {
        if (map[user.login] == null)
            map[user.login] = user
        else {
            val msg = if(byPhone) "A user with this phone already exists" else "A user with this email already exists"
            throw IllegalArgumentException(msg)
        }
    }

    fun loginUser (login: String, password: String) : String? {
        return if (login.isPhoneNumber()) {
            map[login.getAsPhoneNumber()]
        } else {
            map[login.trim()]
        }?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(rawPhone: String?) {
        val phone = rawPhone?.getAsPhoneNumber()
        val plus = phone?.first()?.toString() ?: ""
        val digits = (phone?.length ?: 0) - 1
        if (!plus.equals("+") || digits != Constants.PHONE_LENGTH)
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")

        map[phone].let { user ->
            user?.renewAccessCode()
        }
    }

    fun importUsers(list: List<String>): List<User> {
        val usersList = mutableListOf<User>()
        val filteredList = list.filter { el -> !el.isNullOrBlank() }
        filteredList.forEach {
            User.makeUserFromCsvString(it).also {user ->
                map[user.login] = user
                usersList.add(user)
            }
        }
        return usersList
    }

}
