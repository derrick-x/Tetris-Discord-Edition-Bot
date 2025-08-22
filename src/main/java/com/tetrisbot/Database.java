package com.tetrisbot;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

public class Database {
    static Firestore db; //Connection to Firestore
    static final String FIREBASE_CONFIG = System.getenv("FIREBASE_CONFIG");

    /**
     * Initializes Firestore connection.
     * @throws IOException
     */
    public static void init() throws IOException {
        InputStream serviceAccount = new ByteArrayInputStream(FIREBASE_CONFIG.getBytes());
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();
        FirebaseApp.initializeApp(options);
        db = FirestoreClient.getFirestore();
    }

    /**
     * Writes or overrides a document to Firestore.
     * @param collection The name of the collection.
     * @param document The name of the document.
     * @param entry The map to store in the document.
     * @throws Exception
     */
    public static void write(String collection, String document, Map<String, Object> entry) throws Exception {
        db.collection(collection).document(document).set(entry).get();
    }

    /**
     * Reads and returns a document.
     * @param collection The name of the collection.
     * @param document The name of the document.
     * @return A map representing the document information.
     * @throws Exception
     */
    public static Map<String, Object> read(String collection, String document) throws Exception {
        return db.collection(collection).document(document).get().get().getData();
    }
    /**
     * Reads and returns all documents in a collection.
     * @param collection The name of the collection.
     * @return A mapping of collection name to map representing the document
     * information.
     * @throws Exception
     */
    public static Map<String, Map<String, Object>> read(String collection) throws Exception {
        Map<String, Map<String, Object>> documents = new HashMap<>();
        for (DocumentReference doc : db.collection(collection).listDocuments()) {
            documents.put(doc.getId(), doc.get().get().getData());
        }
        return documents;
    }
}