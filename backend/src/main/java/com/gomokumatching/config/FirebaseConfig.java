package com.gomokumatching.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration class for initializing the Firebase Admin SDK on application startup.
 */
@Configuration
public class FirebaseConfig {

    /**
     * Initializes the Firebase Admin SDK.
     *
     * How it works:
     * 1. It looks for the service account key file in the classpath.
     *    IMPORTANT: The service account key is a secret and must be kept secure.
     *    For production, it's recommended to load this from a secure location or environment variable.
     * 2. It uses the credentials to initialize the FirebaseApp, making it available
     *    for dependency injection throughout the application.
     *
     * @return The initialized FirebaseApp instance.
     * @throws IOException if the service account key file cannot be read.
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // This looks for the file in `src/main/resources`.
        // Make sure to add your service account JSON file there and update the filename.
        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");

        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Initialize the app if it's not already initialized.
            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }
}
