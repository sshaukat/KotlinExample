package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.getAsPhoneNumber
import ru.skillbranch.kotlinexample.extensions.getEmptyAsNull
import ru.skillbranch.kotlinexample.extensions.isPhoneNumber
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor (
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String,Any>? = null
) {
    val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()
    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map {
                it.first().toUpperCase()
            }.joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.getAsPhoneNumber()
            println(field)
        }

    // backing private field
    private var _login: String? = null
    var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!


    private var _salt: String? = null

    private var salt: String
        set(value) {
            _salt = value
        }
        get() {
            if (_salt == null)
                _salt = ByteArray(16).also {
                    SecureRandom().nextBytes(it)
                }.toString()

            return _salt!!
        }

    private val saltOld: String by lazy {

        ByteArray(16).also {
            SecureRandom().nextBytes(it)
        }.toString()
    }
    private lateinit var passwordHash: String

    // only for test purposes
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    // for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ): this(firstName, lastName, email = email, meta = mapOf("auth" to "password"))
    {
        println("Secondary email constructor was called")
        passwordHash = encrypt(password)

    }

    // for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ): this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms"))
    {
        println("Secondary phone constructor was called")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone!!, code)
    }

    // for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        rawPhone: String?,
        salt: String?,
        passwordHash: String?
    ): this(firstName, lastName, email=email, rawPhone=rawPhone, meta = mapOf("src" to "csv"))
    {
        println("Secondary csv constructor was called")
        if (!rawPhone.isNullOrBlank() && rawPhone.isPhoneNumber())
            accessCode = generateAccessCode()
        if (!salt.isNullOrBlank())
            this.salt = salt
        if (!passwordHash.isNullOrBlank())
            this.passwordHash = passwordHash

    }



    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must not be blank!"}
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must not be blank!"}

        phone = rawPhone
        login = email ?: phone!! // one from two definitely is not null!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
            """.trimIndent()
    }

    fun checkPassword(pass: String) = accessCode == pass || encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    fun renewAccessCode() {
        accessCode = generateAccessCode().also { code ->
            sendAccessCodeToUser(phone!!, code)
        }
    }

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIKLMNOPQRSTUVWXYZabcdefghiklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also {index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()
            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() ->
                    User(firstName, lastName, email, password)
                else -> throw  IllegalArgumentException("Email or phone must not be null or blank")

            }
        }

        fun makeUserFromCsvString(csvString: String): User {

            val paramList = csvString.split(";")
            val (firstName, lastName) = paramList[0].fullNameToPair()
            val email = paramList[1].getEmptyAsNull()
            val (salt, hash) = paramList[2].saltHashToPair()
            val phone = paramList[3].getEmptyAsNull()

            if (hash == null)
                throw IllegalArgumentException("user password hash is null")

            return User(firstName, lastName, email, phone, salt, hash)
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "FullName must contain only first name " +
                                    " and last name, current split result ${this@fullNameToPair}"
                        )
                    }

                }

        }

        private fun String.saltHashToPair(): Pair<String, String?> {
            return this.split(":")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "password hash and salt field must contain only first name " +
                                    " and last name, current split result ${this@saltHashToPair}"
                        )
                    }

                }


        }

    }

}


