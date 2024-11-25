package com.example.musicplayer2.utils;

import android.net.Uri;
import android.util.Log;

import com.example.musicplayer2.models.Music;
import com.example.musicplayer2.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseUtils {

    private static final String TAG = "FirebaseUtils";
    private static final String MUSIC_COLLECTION = "music";
    private static final String PLAYLIST_COLLECTION = "playlists";
    private static final String USER_COLLECTION = "users";
    // Firebase instances
    private static FirebaseAuth auth = FirebaseAuth.getInstance();
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * Uploads music to Firebase Storage and retrieves the download URL.
     *
     * @param fileUri            the URI of the music file
     * @param title              the title of the music
     * @param genre              the genre of the music
     * @param imageUrl           the URL of the uploaded music image file
     * @param onCompleteListener listener for when the upload is complete
     */
    public static void uploadMusic(Uri fileUri, String title, String genre, String imageUrl, OnCompleteListener<Uri> onCompleteListener) {
        String userId = auth.getCurrentUser().getUid();
        String musicId = db.collection(MUSIC_COLLECTION).document().getId();
        StorageReference storageReference = storage.getReference().child("music/" + musicId);

        Log.d(TAG, "Uploading music for user: " + userId);

        // Upload the music file
        storageReference.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Music upload failed: ", task.getException());
                        throw task.getException();
                    }
                    // Return the file URL after upload is complete
                    return storageReference.getDownloadUrl();
                })
                .addOnCompleteListener(onCompleteListener)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get download URL: ", e));
    }

    /**
     * Uploads music image to Firebase Storage and retrieves the download URL.
     *
     * @param imageUri           the URI of the music image file
     * @param onCompleteListener listener for when the upload is complete
     */
    public static void uploadMusicImage(Uri imageUri, OnCompleteListener<String> onCompleteListener) {
        String imageId = db.collection(MUSIC_COLLECTION).document().getId();
        StorageReference imageRef = storage.getReference().child("music_images/" + imageId);

        imageRef.putFile(imageUri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return imageRef.getDownloadUrl();
            })
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String imageUrl = task.getResult().toString();
                    onCompleteListener.onComplete(Tasks.forResult(imageUrl));
                } else {
                    onCompleteListener.onComplete(Tasks.forResult(""));
                }
            });
    }

    /**
     * Saves music metadata to Firestore.
     *
     * @param title              the title of the music
     * @param genre              the genre of the music
     * @param fileUrl            the URL of the uploaded music file
     * @param imageUrl           the URL of the uploaded music image file
     * @param onCompleteListener listener for when the save operation is complete
     */
    public static void saveMusicData(String title, String genre, String fileUrl, String imageUrl, OnCompleteListener<Void> onCompleteListener) {
        String userId = auth.getCurrentUser().getUid();
        String musicId = db.collection(MUSIC_COLLECTION).document().getId();

        // Get user's name from Firestore
        db.collection(USER_COLLECTION).document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                String artistName = documentSnapshot.getString("name");
                Music music = new Music(musicId, title, artistName, fileUrl, userId, imageUrl, genre);
                db.collection(MUSIC_COLLECTION).document(musicId)
                    .set(music)
                    .addOnCompleteListener(onCompleteListener);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user name: ", e);
                onCompleteListener.onComplete(Tasks.forException(e));
            });
    }

    /**
     * Retrieves the music collection from Firestore.
     *
     * @return the Firestore collection reference
     */
    public static CollectionReference getMusicCollection() {
        return db.collection(MUSIC_COLLECTION);
    }

    /**
     * Loads music data from Firestore.
     *
     * @param onCompleteListener listener for when the music data is retrieved
     */
    public static void loadMusicFromFirestore(OnCompleteListener<QuerySnapshot> onCompleteListener) {
        db.collection(MUSIC_COLLECTION)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Music music = doc.toObject(Music.class);
                        if (music != null) {
                            music.setMusicId(doc.getId()); // Ensure musicId is set
                            // Add music to your list
                        }
                    }
                    onCompleteListener.onComplete(task);
                } else {
                    onCompleteListener.onComplete(task);
                }
            });
    }

    public static void createUserProfile(String userId, String email, String name, OnCompleteListener<Void> listener) {
        User newUser = new User();
        newUser.setUserId(userId);
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setPlaylistIds(new ArrayList<>()); // Initialize empty lists
        newUser.setUploadedSongs(new ArrayList<>());
        newUser.setImageUrl(""); // Default empty image URL

        db.collection(USER_COLLECTION)
                .document(userId)
                .set(newUser)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error creating user profile: ", e));
    }

    public static void checkUserExists(String userId, OnCompleteListener<Boolean> listener) {
        db.collection(USER_COLLECTION)
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean exists = task.getResult() != null && task.getResult().exists();
                        listener.onComplete(Tasks.forResult(exists));
                    } else {
                        listener.onComplete(Tasks.forResult(false));
                    }
                });
    }

    /**
     * Adds a new playlist to Firestore.
     *
     * @param playlistName       the name of the playlist
     * @param onCompleteListener listener for when the playlist is created
     */
    public static void addPlaylist(String playlistName, OnCompleteListener<Void> onCompleteListener) {
        String userId = auth.getCurrentUser().getUid();
        String playlistId = db.collection(PLAYLIST_COLLECTION).document().getId();

        // Create a playlist entry - Match field names with Playlist.java
        Map<String, Object> playlistData = new HashMap<>();
        playlistData.put("playlistId", playlistId);  // Make sure this matches the field name
        playlistData.put("name", playlistName);
        playlistData.put("userId", userId);  // Match field name
        playlistData.put("musicIds", new ArrayList<>());  // Match field name
        playlistData.put("imageUrl", "");  // Match field name
        playlistData.put("createdAt", System.currentTimeMillis());

        db.collection(PLAYLIST_COLLECTION).document(playlistId).set(playlistData)
                .addOnCompleteListener(onCompleteListener)
                .addOnFailureListener(e -> Log.e(TAG, "Error creating playlist: ", e));
    }
    
    public static void createPlaylist(String name, OnCompleteListener<Void> listener) {
        String userId = auth.getCurrentUser().getUid();
        String playlistId = db.collection(PLAYLIST_COLLECTION).document().getId();

        Map<String, Object> playlist = new HashMap<>();
        playlist.put("playlistId", playlistId);  // Changed from "id" to "playlistId"
        playlist.put("name", name);
        playlist.put("userId", userId);
        playlist.put("musicIds", new ArrayList<>());  // Changed from "songs" to "musicIds"
        playlist.put("imageUrl", "");
        playlist.put("createdAt", System.currentTimeMillis());

        db.collection(PLAYLIST_COLLECTION).document(playlistId)
            .set(playlist)
            .addOnCompleteListener(listener);
    }

    public static void searchSongs(String query, OnCompleteListener<List<Music>> listener) {
        db.collection(MUSIC_COLLECTION)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<Music> results = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        Music music = doc.toObject(Music.class);
                        if (music != null && music.getTitle() != null && 
                            music.getTitle().toLowerCase().contains(query.toLowerCase())) {
                            results.add(music);
                        }
                    }
                    listener.onComplete(Tasks.forResult(results));
                } else {
                    listener.onComplete(Tasks.forException(task.getException()));
                }
            });
    }

    public static void addSongToPlaylist(String playlistId, String musicId, OnCompleteListener<Void> listener) {
        db.collection(PLAYLIST_COLLECTION).document(playlistId)
            .update("musicIds", FieldValue.arrayUnion(musicId))
            .addOnCompleteListener(listener);
    }

    public static void removeSongFromPlaylist(String playlistId, String musicId, OnCompleteListener<Void> listener) {
        // Change from "songs" to "musicIds"
        db.collection(PLAYLIST_COLLECTION).document(playlistId)
            .update("musicIds", FieldValue.arrayRemove(musicId))
            .addOnCompleteListener(listener);
    }

    public static void getPlaylistSongs(String playlistId, OnCompleteListener<List<Music>> listener) {
        db.collection(PLAYLIST_COLLECTION).document(playlistId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                // Change from "songs" to "musicIds" to match the field name
                List<String> musicIds = (List<String>) documentSnapshot.get("musicIds");
                if (musicIds == null || musicIds.isEmpty()) {
                    listener.onComplete(Tasks.forResult(new ArrayList<>()));
                    return;
                }

                db.collection(MUSIC_COLLECTION)
                    .whereIn("musicId", musicIds)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Music> songs = new ArrayList<>();
                            for (DocumentSnapshot doc : task.getResult()) {
                                Music music = doc.toObject(Music.class);
                                if (music != null) {
                                    songs.add(music);
                                }
                            }
                            listener.onComplete(Tasks.forResult(songs));
                        } else {
                            listener.onComplete(Tasks.forException(task.getException()));
                        }
                    });
            })
            .addOnFailureListener(e -> listener.onComplete(Tasks.forException(e)));
    }

    /**
     * Logs user interactions with songs.
     *
     * @param userId The ID of the user.
     * @param musicId The ID of the music.
     */
    public static void logUserInteraction(String userId, String musicId) {
        DocumentReference userRef = db.collection("user_interactions").document(userId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("interactions." + musicId, FieldValue.increment(1));

        userRef.set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                // Successfully logged interaction
                Log.d(TAG, "User interaction logged for userId: " + userId + ", musicId: " + musicId);
                generateRecommendations(listener);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error logging user interaction: ", e);
            });
    }

    public static void generateRecommendations(OnCompleteListener<Void> listener) {
        // Call the HTTP triggered Cloud Function
        functions.getHttpsCallable("generateRecommendations")
            .call()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Successfully triggered recommendations generation");
                    listener.onComplete(Tasks.forResult(null));
                } else {
                    Log.e(TAG, "Failed to trigger recommendations generation", task.getException());
                    listener.onComplete(Tasks.forException(task.getException()));
                }
            });
    }
    /**
     * Retrieves user recommendations from Firestore.
     *
     * @param userId  The user's ID.
     * @param listener  The listener to handle the result.
     */
    public static void getUserRecommendations(String userId, OnCompleteListener<List<Music>> listener) {
        db.collection("user_recommendations")
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.exists()) {
                        List<Map<String, Object>> recommendationsData = (List<Map<String, Object>>) doc.get("recommendations");
                        List<Music> recommendations = new ArrayList<>();
                        if (recommendationsData != null) {
                            for (Map<String, Object> musicData : recommendationsData) {
                                Music music = new Music();
                                music.setMusicId((String) musicData.get("musicId"));
                                music.setTitle((String) musicData.get("title"));
                                music.setArtist((String) musicData.get("artist"));
                                music.setFileUrl((String) musicData.get("fileUrl"));
                                music.setImageUrl((String) musicData.get("imageUrl"));
                                music.setGenre((String) musicData.get("genre"));
                                // Set other fields if necessary
                                recommendations.add(music);
                            }
                        }
                        listener.onComplete(Tasks.forResult(recommendations));
                    } else {
                        listener.onComplete(Tasks.forResult(new ArrayList<>()));
                    }
                } else {
                    listener.onComplete(Tasks.forException(task.getException()));
                }
            });
    }
}
