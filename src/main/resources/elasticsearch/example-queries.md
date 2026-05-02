# Elasticsearch DSL — example queries

Index: `iwas-users`

## 1. Autocomplete (prefix on fullName + position)
```json
GET iwas-users/_search
{
  "size": 10,
  "query": {
    "bool": {
      "should": [
        { "match_phrase_prefix": { "fullName": "joh" } },
        { "match_phrase_prefix": { "position": "joh" } }
      ],
      "filter": [ { "term": { "isActive": true } } ]
    }
  },
  "sort": [ "_score" ]
}
```

## 2. Full-text search with fuzziness (typo tolerance)
```json
GET iwas-users/_search
{
  "from": 0,
  "size": 20,
  "query": {
    "bool": {
      "should": [
        {
          "multi_match": {
            "query":   "jhon developer",
            "fields":  ["fullName^3", "position^2", "email"],
            "fuzziness": "AUTO"
          }
        },
        { "match_phrase_prefix": { "fullName": { "query": "jhon developer", "boost": 2 } } }
      ],
      "filter": [ { "term": { "isActive": true } } ]
    }
  }
}
```

## 3. Index settings (custom analyzer for edge_ngram)
See `iwas-users-settings.json`. Index-time analyzer = `edge_ngram_analyzer`,
search-time analyzer = `standard` so user input isn't itself ngram-tokenized.

## 4. Reindex a single user (used by sync listener)
```json
PUT iwas-users/_doc/42
{
  "id": 42,
  "email": "alice@workforce.com",
  "fullName": "Alice Tran",
  "position": "Senior Backend Engineer",
  "role": "TEAM_MEMBER",
  "avatarUrl": null,
  "isActive": true
}
```
