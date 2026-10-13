// MongoDB initialization script
// Creates the database and collections with indexes

db = db.getSiblingDB('recsys');

// Create articles collection with indexes
db.createCollection('articles');

// Create indexes for common query patterns
db.articles.createIndex({ "_id": 1 }, { unique: true });
db.articles.createIndex({ "hl": "text", "syn": "text" }); // Text search index
db.articles.createIndex({ "createdAt": -1 }); // Time-based queries
db.articles.createIndex({ "seolocation": 1 }); // URL lookups

// Create a view for smart tips (lightweight)
db.createView('smart_tips', 'articles', [
    { $project: { _id: 1, hl: 1, seolocation: 1 } }
]);

// Create a view for summaries
db.createView('summaries', 'articles', [
    { $project: { _id: 1, hl: 1, syn: 1 } }
]);

print('MongoDB initialization complete: recsys database with articles collection and indexes created.');