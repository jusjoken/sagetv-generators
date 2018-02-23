package tv.sage.tools.api;

/**
 * Created by seans on 16/03/17.
 */
public class APIHelper {
    static class Property {
        public String name;
        public String type;

        public Property(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    public static Property addProperty(String name, String type) {
        return new Property(name, type);
    }

    public static Property[] METADATA_PROPERTIES = {
            addProperty("Title", "string"),
            addProperty("EpisodeName", "string"),
            addProperty("Genre", "array"),
            addProperty("GenreID", "string"),
            addProperty("Description", "string"),
            addProperty("Year", "integer"),
            addProperty("Language", "string"),
            addProperty("Rated", "string"),
            addProperty("ParentalRating", "string"),
            addProperty("RunningTime", "string"),
            addProperty("OriginalAirDate", "integer"),
            addProperty("ExtendedRatings", "string"),
            addProperty("Misc", "string"),
            addProperty("PartNumber", "integer"),
            addProperty("TotalParts", "integer"),
            addProperty("HDTV", "boolean"),
            addProperty("CC", "boolean"),
            addProperty("Stereo", "boolean"),
            addProperty("Subtitled", "boolean"),
            addProperty("Premiere", "boolean"),
            addProperty("SeasonPremiere", "boolean"),
            addProperty("SeriesPremiere", "boolean"),
            addProperty("ChannelPremiere", "boolean"),
            addProperty("SeasonFinal", "boolean"),
            addProperty("SeriesFinale", "boolean"),
            addProperty("SAP", "boolean"),
            addProperty("ExternalID", "string"),
            addProperty("Width", "integer"),
            addProperty("Height", "integer"),
            addProperty("Track", "integer"),
            addProperty("TotalTracks", "integer"),
            addProperty("Comment", "string"),
            addProperty("AiringTime", "date"),
            addProperty("ThumbnailOffset", "integer"),
            addProperty("ThumbnailSize", "integer"),
            addProperty("ThumbnailDesc", "string"),
            addProperty("Duration", "integer"),
            addProperty("Picture.Resolution", "string"),
            addProperty("MediaTitle", "string"),
            addProperty("MediaType", "string"),
            addProperty("SeasonNumber", "integer"),
            addProperty("EpisodeNumber", "string"),
            addProperty("IMDBID", "string"),
            addProperty("DiscNumber", "string"),
            addProperty("MediaProviderID", "string"),
            addProperty("MediaProviderDataID", "string"),
            addProperty("UserRating", "integer"),
            addProperty("Fanart", "array"),
            addProperty("TrailerUrl", "string"),
            addProperty("SeriesInfoID", "integer"),
            addProperty("EpisodeCount", "integer"),
            addProperty("CollectionName", "string"),
            addProperty("CollectionID", "integer"),
            addProperty("CollectionOverview", "string"),
            addProperty("DefaultPoster", "string"),
            addProperty("DefaultBanner", "string"),
            addProperty("DefaultBackground", "string"),
            addProperty("ScrapedBy", "string"),
            addProperty("ScrapedDate", "long"),
            addProperty("TagLine", "string"),
            addProperty("Quotes", "string"),
            addProperty("Trivia", "string")
    };

    String packageName;

    public String getPackageName() {
        return packageName;
    }

    public APIHelper setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }
}
