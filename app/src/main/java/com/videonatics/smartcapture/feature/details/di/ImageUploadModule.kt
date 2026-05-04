package com.videonatics.smartcapture.feature.details.di

import android.content.Context
import com.videonatics.smartcapture.core.MoireEffectClassifier
import com.videonatics.smartcapture.core.loadFatBoxLabels
import com.videonatics.smartcapture.core.loadFatBoxModel
import com.videonatics.smartcapture.core.loadMoireModel
import com.videonatics.smartcapture.feature.details.FatBoxClassifier
import com.videonatics.smartcapture.feature.details.data.repository.ImageUploadRepositoryImpl
import com.videonatics.smartcapture.feature.details.domain.repository.ImageUploadRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageUploadModule {

    @Provides
    @Singleton
    fun provideFatBoxClassifier(@ApplicationContext context: Context): FatBoxClassifier {
        return FatBoxClassifier(
            interpreter = loadFatBoxModel(context),
            labels = loadFatBoxLabels(context)
        )
    }

    @Provides
    @Singleton
    fun provideMoireEffectClassifier(@ApplicationContext context: Context): MoireEffectClassifier {
        val interpreter = loadMoireModel(context)
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()

        println("INPUT SHAPE: ${inputShape.contentToString()}")
        println("OUTPUT SHAPE: ${outputShape.contentToString()}")
        return MoireEffectClassifier(
            interpreter = interpreter
        )
    }

    @Provides
    @Singleton
    fun provideImageUploadRepository(
        client: HttpClient,
        classifier: FatBoxClassifier,
        moireEffectClassifier: MoireEffectClassifier
    ): ImageUploadRepository {
        return ImageUploadRepositoryImpl(client, classifier, moireEffectClassifier)
    }
}
