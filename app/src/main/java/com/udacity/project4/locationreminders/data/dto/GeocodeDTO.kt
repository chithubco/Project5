package com.udacity.project4.locationreminders.data.dto

import android.os.Parcel
import android.os.Parcelable


data class GeocodeDTO(
    val location: String?,
    val latitude: String?,
    val longitude: String?
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(location)
        parcel.writeString(latitude)
        parcel.writeString(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GeocodeDTO> {
        override fun createFromParcel(parcel: Parcel): GeocodeDTO {
            return GeocodeDTO(parcel)
        }

        override fun newArray(size: Int): Array<GeocodeDTO?> {
            return arrayOfNulls(size)
        }
    }
}