package com.videonatics.smartcapture.core.data.datasource.local

import com.videonatics.smartcapture.core.Pref
import javax.inject.Inject

interface LocalCoreDataSource {
}

class LocalCoreDataSourceImpl @Inject constructor(
    val pref: Pref
) : LocalCoreDataSource{

}