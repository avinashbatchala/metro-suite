package com.metro.people.data.import

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns

/**
 * Orchestrates a VCF import: parse → duplicate analysis → `ContactsContract` write. Keeps parsing
 * and provider access out of the Compose layer, and never logs contact data.
 */
class ContactImportRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun parse(uri: Uri): ParseResult {
        val stream = try {
            resolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        } ?: return ParseResult.Failure(ParseFailure.UNREADABLE)
        return try {
            stream.use { VCardParser.parse(it) }
        } catch (e: Exception) {
            ParseResult.Failure(ParseFailure.UNREADABLE)
        }
    }

    fun displayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return try {
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun buildPreview(contacts: List<ImportContact>, warningCount: Int, fileName: String?): ImportPreview {
        val detector = ContactDuplicateDetector(loadExistingIndex())
        val classified = contacts.map { ClassifiedImport(it, detector.classify(it)) }
        return ImportPreview(classified, warningCount, fileName)
    }

    fun import(
        importable: List<ClassifiedImport>,
        skippedExact: Int,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportResult {
        val writer = ContactWriter(resolver)
        val success = writer.write(importable.map { it.contact }, onProgress)
        var added = 0
        var possibleAdded = 0
        var failed = 0
        importable.forEachIndexed { index, classified ->
            if (success.getOrElse(index) { false }) {
                if (classified.classification == DuplicateClass.POSSIBLE_DUPLICATE) {
                    possibleAdded++
                } else {
                    added++
                }
            } else {
                failed++
            }
        }
        return ImportResult(added, possibleAdded, skippedDuplicates = skippedExact, failed = failed)
    }

    fun loadExistingIndex(): ExistingContactIndex {
        val phones = runCatching {
            val result = ArrayList<String>()
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null,
            )?.use { cursor ->
                val column = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val digits = PhoneNormalizer.digitsOnly(cursor.getString(column))
                    if (digits.length >= 7) result.add(digits)
                }
            }
            result
        }.getOrDefault(emptyList())

        val emails = runCatching {
            val result = HashSet<String>()
            resolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                null,
                null,
                null,
            )?.use { cursor ->
                val column = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (cursor.moveToNext()) {
                    val email = cursor.getString(column)?.let { ContactDuplicateDetector.normalizeEmail(it) }
                    if (!email.isNullOrEmpty()) result.add(email)
                }
            }
            result
        }.getOrDefault(emptySet())

        val names = runCatching {
            val result = HashSet<String>()
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                null,
            )?.use { cursor ->
                val column = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(column)?.let { ContactDuplicateDetector.normalizeName(it) }
                    if (!name.isNullOrEmpty()) result.add(name)
                }
            }
            result
        }.getOrDefault(emptySet())

        return ExistingContactIndex(phones, emails, names)
    }
}
