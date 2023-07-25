package com.idrnd.idvoice.utils.license

import android.util.Log
import net.idrnd.voicesdk.android.extra.MobileLicense
import net.idrnd.voicesdk.common.VoiceSdkEngineException
import net.idrnd.voicesdk.core.BuildInfo
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Information about a license of ID R&D product.
 *
 * @param license Content of a license file.
 */
class IdrndLicense(license: String) {

    /**
     * Return the license status.
     */
    val licenseStatus: LicenseStatus

    /**
     * Return the expiration date of license. If return null it means that the license is invalid.
     */
    var expirationDate: Date? = null
        private set

    /**
     * String representation of the expiration date. For example 2024-12-31.
     */
    val stringExpirationDate: String?
        get() {
            if (expirationDate == null) return null
            return dateFormat.format(expirationDate)
        }

    init {
        licenseStatus = setLicense(license)
    }

    private fun setLicense(license: String): LicenseStatus {
        try {
            MobileLicense.setLicense(license)
        } catch (e: VoiceSdkEngineException) {
            Log.e(TAG, "MobileLicense throws exception when it sets the license", e)
            return LicenseStatus.Invalid
        }

        val stringDate = BuildInfo.get().licenseInfo.split(" ").last()
        val optionalExpirationDate = try {
            dateFormat.parse(stringDate)
        } catch (e: ParseException) {
            Log.e(TAG, "License info string is changed and we can't parse it", e)
            return LicenseStatus.Invalid
        }

        if (optionalExpirationDate == null) {
            Log.e(TAG, "Expiration date is null")
            return LicenseStatus.Invalid
        }

        expirationDate = optionalExpirationDate
        val currentDate = Date(System.currentTimeMillis())

        if (expirationDate!!.before(currentDate)) {
            Log.i(TAG, "License is expired")
            return LicenseStatus.Expired
        }

        Log.i(TAG, "License is valid")
        return LicenseStatus.Valid
    }

    companion object {
        private val TAG = IdrndLicense::class.simpleName
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
}
