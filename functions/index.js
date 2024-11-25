const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Changed to HTTP trigger instead of pubsub schedule
exports.generateRecommendations = functions.https.onRequest(async (req, res) => {
    // Add basic security check
    if (req.method !== 'POST') {
        res.status(405).send('Method Not Allowed');
        return;
    }

    const db = admin.firestore();

    try {
        // Rest of the code remains the same
        const userInteractionsSnapshot = await db.collection('user_interactions').get();
        if (userInteractionsSnapshot.empty) {
            console.log('No user interactions found');
            res.status(200).json({ message: 'No user interactions found' });
            return;
        }

        const userInteractions = {};
        userInteractionsSnapshot.forEach(doc => {
            if (doc.exists) {
                userInteractions[doc.id] = doc.data().interactions || {};
            }
        });

        // Build item-user interaction matrix with validation
        const itemUserMatrix = {};
        for (const userId in userInteractions) {
            const interactions = userInteractions[userId];
            if (typeof interactions === 'object' && interactions !== null) {
                for (const itemId in interactions) {
                    if (!itemUserMatrix[itemId]) {
                        itemUserMatrix[itemId] = {};
                    }
                    itemUserMatrix[itemId][userId] = Number(interactions[itemId]) || 0;
                }
            }
        }

        // Compute item-item similarity with additional checks
        const itemSimilarities = {};
        const itemIds = Object.keys(itemUserMatrix);
        
        if (itemIds.length === 0) {
            console.log('No items found in the interaction matrix');
            res.status(200).json({ message: 'No items found in the interaction matrix' });
            return;
        }

        for (let i = 0; i < itemIds.length; i++) {
            const itemId1 = itemIds[i];
            if (!itemSimilarities[itemId1]) {
                itemSimilarities[itemId1] = {};
            }
            
            for (let j = i + 1; j < itemIds.length; j++) {
                const itemId2 = itemIds[j];
                if (!itemSimilarities[itemId2]) {
                    itemSimilarities[itemId2] = {};
                }
                
                const similarity = cosineSimilarity(itemUserMatrix[itemId1], itemUserMatrix[itemId2]);
                if (similarity > 0) {
                    itemSimilarities[itemId1][itemId2] = similarity;
                    itemSimilarities[itemId2][itemId1] = similarity;
                }
            }
        }

        // Generate recommendations with batch writes
        const batchPromises = [];
        let currentBatch = db.batch();
        let operationCount = 0;
        const MAX_BATCH_SIZE = 500;

        for (const userId in userInteractions) {
            const interactions = userInteractions[userId];
            const scores = {};

            for (const itemId in interactions) {
                const itemScore = Number(interactions[itemId]) || 0;
                const similarItems = itemSimilarities[itemId] || {};
                
                for (const similarItemId in similarItems) {
                    if (!interactions[similarItemId]) {
                        scores[similarItemId] = (scores[similarItemId] || 0) + 
                            similarItems[similarItemId] * itemScore;
                    }
                }
            }

            const recommendedItemIds = Object.keys(scores)
                .sort((a, b) => scores[b] - scores[a])
                .slice(0, 20);

            if (recommendedItemIds.length === 0) {
                continue;
            }

            try {
                const CHUNK_SIZE = 10;
                const recommendedSongs = [];
                
                for (let i = 0; i < recommendedItemIds.length; i += CHUNK_SIZE) {
                    const chunk = recommendedItemIds.slice(i, i + CHUNK_SIZE);
                    const musicDocs = await db.collection('music')
                        .where('musicId', 'in', chunk)
                        .get();
                    
                    recommendedSongs.push(...musicDocs.docs.map(doc => doc.data()));
                }

                const userRecommendationRef = db.collection('user_recommendations').doc(userId);
                currentBatch.set(userRecommendationRef, { 
                    recommendations: recommendedSongs,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
                
                operationCount++;

                if (operationCount >= MAX_BATCH_SIZE) {
                    batchPromises.push(currentBatch.commit());
                    currentBatch = db.batch();
                    operationCount = 0;
                }
            } catch (error) {
                console.error(`Error processing recommendations for user ${userId}:`, error);
                continue;
            }
        }

        if (operationCount > 0) {
            batchPromises.push(currentBatch.commit());
        }

        await Promise.all(batchPromises);

        console.log('Recommendations generated successfully.');
        res.status(200).json({ message: 'Recommendations generated successfully' });
    } catch (error) {
        console.error('Error generating recommendations:', error);
        res.status(500).json({ error: 'Failed to generate recommendations' });
    }
});

function cosineSimilarity(vecA, vecB) {
    try {
        const intersection = Object.keys(vecA).filter(key => key in vecB);
        if (intersection.length === 0) {
            return 0;
        }

        let dotProduct = 0;
        let magnitudeA = 0;
        let magnitudeB = 0;

        for (const key in vecA) {
            const valueA = Number(vecA[key]) || 0;
            magnitudeA += valueA * valueA;

            if (key in vecB) {
                const valueB = Number(vecB[key]) || 0;
                dotProduct += valueA * valueB;
            }
        }

        for (const value of Object.values(vecB)) {
            const valueB = Number(value) || 0;
            magnitudeB += valueB * valueB;
        }

        magnitudeA = Math.sqrt(magnitudeA);
        magnitudeB = Math.sqrt(magnitudeB);

        if (magnitudeA === 0 || magnitudeB === 0) {
            return 0;
        }

        return dotProduct / (magnitudeA * magnitudeB);
    } catch (error) {
        console.error('Error in cosineSimilarity calculation:', error);
        return 0;
    }
}