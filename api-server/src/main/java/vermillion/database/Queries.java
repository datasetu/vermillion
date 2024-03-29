package vermillion.database;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Queries {

  public JsonObject baseQuery;
  public JsonArray filterQuery;
  public JsonObject timeQuery;
  public JsonObject geoQuery;
  public JsonObject rangeQuery;
  public JsonObject termQuery;
  public JsonObject downloadByQuery;
  public JsonObject providerByQuery;
  public JsonObject termsQuery;

  public Queries() {

    downloadByQuery = new JsonObject()
            .put("query",
                    new JsonObject().put("bool",
                            new JsonObject().put("must",
                                    new JsonObject().put("bool",
                                            new JsonObject().put("should", new JsonArray())))));
    providerByQuery =
            new JsonObject()
                    .put("query", new JsonObject().put("bool", new JsonObject().put("filter", new JsonArray())));
    baseQuery =
        new JsonObject()
            .put("query", new JsonObject().put("bool", new JsonObject().put("filter", "")));

    filterQuery = new JsonArray();

    timeQuery =
        new JsonObject()
            .put(
                "range",
                new JsonObject().put("timestamp", new JsonObject().put("gte", "").put("lte", "")));

    geoQuery =
        new JsonObject()
            .put(
                "geo_distance",
                new JsonObject().put("distance", "0km").put("coordinates", new JsonArray()));

    rangeQuery = new JsonObject().put("range", new JsonObject());

    // For multiple terms query, the termValue element should be a JsonArray
    termQuery = new JsonObject().put("term", new JsonObject());

    // Used only for Multi-ID queries
    termsQuery = new JsonObject().put("terms", new JsonObject().put("id.keyword", new JsonArray()));
  }

  public JsonObject getBaseQuery() {
    return baseQuery;
  }

  public JsonArray getFilterQuery() {
    return filterQuery;
  }

  public JsonObject getTimeQuery() {
    return timeQuery;
  }

  public JsonObject getGeoQuery() {
    return geoQuery;
  }

  public JsonObject getRangeQuery() {
    return rangeQuery;
  }

  public JsonObject getDownloadByQuery() { return downloadByQuery; }

  public JsonObject getLatestQuery(JsonObject queryObject) {
    return queryObject
        .put("size", 1)
        .put(
            "sort",
            new JsonArray()
                .add(new JsonObject().put("timestamp", new JsonObject().put("order", "desc"))));
  }

  public JsonObject getTermQuery() {
    return termQuery;
  }

  public JsonObject getTermsQuery() {
    return termsQuery;
  }

  public JsonObject getProviderByQuery() { return providerByQuery; }

}
