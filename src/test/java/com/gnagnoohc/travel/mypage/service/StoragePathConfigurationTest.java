package com.gnagnoohc.travel.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;

class StoragePathConfigurationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void profileStorageUsesMypageUploadProperty() throws Exception {
        assertConstructorProperty(
                BusinessMediaStorage.class, "${file.upload-mypage}");

        Path mypageDirectory = tempDirectory.resolve("mypage");
        BusinessMediaStorage storage =
                new BusinessMediaStorage(mypageDirectory.toString());
        MockMultipartFile profile = new MockMultipartFile(
                "profileImage", "profile.png", "image/png",
                new byte[] {1, 2, 3});

        String imageUrl = storage.storeProfile(profile, 1L);
        String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

        assertThat(storage.getRootDirectory()).isEqualTo(
                mypageDirectory.toAbsolutePath().normalize());
        assertThat(Files.isRegularFile(mypageDirectory.resolve(fileName)))
                .isTrue();
    }

    @Test
    void businessDocumentStorageUsesAuthUploadProperty() throws Exception {
        assertConstructorProperty(
                BusinessDocumentStorage.class, "${file.upload-auth}");

        Path authDirectory = tempDirectory.resolve("auth");
        BusinessDocumentStorage storage =
                new BusinessDocumentStorage(authDirectory.toString());
        MockMultipartFile document = new MockMultipartFile(
                "document", "registration.pdf", "application/pdf",
                new byte[] {4, 5, 6});

        String documentUrl = storage.store(document, 2L);
        String fileName = documentUrl.substring(
                documentUrl.lastIndexOf('/') + 1);

        assertThat(Files.isRegularFile(authDirectory.resolve(fileName)))
                .isTrue();
    }

    private void assertConstructorProperty(
            Class<?> storageType, String expectedProperty) {
        Constructor<?> constructor = storageType.getDeclaredConstructors()[0];
        Value value = constructor.getParameters()[0].getAnnotation(Value.class);

        assertThat(value).isNotNull();
        assertThat(value.value()).isEqualTo(expectedProperty);
    }
}
