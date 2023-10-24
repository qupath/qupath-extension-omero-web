package qupath.lib.images.servers.omero.core.entities.repositoryentities.serverentities;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.omero.core.apis.ApisHandler;
import qupath.lib.images.servers.omero.core.entities.permissions.Group;
import qupath.lib.images.servers.omero.core.entities.permissions.Owner;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.serverentities.image.Image;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A server entity represents an OMERO entity belonging to the project/dataset/image
 * hierarchy.
 */
public abstract class ServerEntity extends RepositoryEntity {

    private static final Logger logger = LoggerFactory.getLogger(ServerEntity.class);
    @SerializedName(value = "@id") protected long id;
    @SerializedName(value = "Name") protected String name;
    private Owner owner = Owner.getAllMembersOwner();
    private Group group = Group.getAllGroupsGroup();

    protected ServerEntity() {}

    @Override
    public String getName() {
        return name == null ? "" : name;
    }

    @Override
    public boolean isFilteredByGroupOwnerName(Group groupFilter, Owner ownerFilter, String nameFilter) {
        return (groupFilter == null || groupFilter == Group.getAllGroupsGroup() || group.equals(groupFilter)) &&
                (ownerFilter == null || ownerFilter == Owner.getAllMembersOwner() || owner.equals(ownerFilter)) &&
                filterProjectsByName(nameFilter);
    }

    /**
     * @return a textual description of the entity. This is needed in API requests for example
     */
    abstract public String getType();

    /**
     * Returns the <b>name</b> of an attribute associated with this entity.
     *
     * @param informationIndex the index of the attribute
     * @return the attribute name corresponding to the index, or an empty String if the index is out of bound
     */
    abstract public String getAttributeInformation(int informationIndex);

    /**
     * Returns the <b>value</b> of an attribute associated with this entity.
     *
     * @param informationIndex the index of the attribute
     * @return the attribute value corresponding to the index, or an empty String if the index is out of bound
     */
    abstract public String getValueInformation(int informationIndex);

    /**
     * @return the total number of attributes this entity has
     */
    abstract public int getNumberOfAttributes();

    /**
     * Creates a stream of entities from a list of JSON elements.
     * If an entity cannot be created from a JSON element, it is discarded.
     *
     * @param jsonElements  the JSON elements supposed to represent server entities
     * @param apisHandler the request handler of the browser
     * @return a stream of server entities
     */
    public static Stream<ServerEntity> createFromJsonElements(List<JsonElement> jsonElements, ApisHandler apisHandler) {
        return jsonElements.stream()
                .map(jsonElement -> createFromJsonElement(jsonElement, apisHandler))
                .flatMap(Optional::stream);
    }

    /**
     * Creates a server entity from a JSON element.
     *
     * @param jsonElement  the JSON element supposed to represent a server entity
     * @param apisHandler the request handler of the browser
     * @return a server entity, or an empty Optional if it was impossible to create
     */
    public static Optional<ServerEntity> createFromJsonElement(JsonElement jsonElement, ApisHandler apisHandler) {
        Gson deserializer = new GsonBuilder().registerTypeAdapter(ServerEntity.class, new ServerEntityDeserializer(apisHandler)).setLenient().create();

        try {
            return Optional.of(deserializer.fromJson(jsonElement, ServerEntity.class));
        } catch (JsonSyntaxException e) {
            logger.error("Error when deserializing " + jsonElement, e);
            return Optional.empty();
        }
    }

    /**
     * @return the OMERO ID associated with this entity
     */
    public long getId() {
        return id;
    }

    /**
     * @return the OMERO owner of this entity
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * @return the OMERO group of this entity
     */
    public Group getGroup() {
        return group;
    }

    private record ServerEntityDeserializer(ApisHandler apisHandler) implements JsonDeserializer<ServerEntity> {
        @Override
        public ServerEntity deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                String type = json.getAsJsonObject().get("@type").getAsString().toLowerCase();

                ServerEntity serverEntity = null;
                if (Image.isImage(type)) {
                    serverEntity = context.deserialize(json, Image.class);
                    ((Image) serverEntity).setWebClient(apisHandler.getClient());
                } else if (Dataset.isDataset(type)) {
                    serverEntity = context.deserialize(json, Dataset.class);
                    ((Dataset) serverEntity).setApisHandler(apisHandler);
                } else if (Project.isProject(type)) {
                    serverEntity = context.deserialize(json, Project.class);
                    ((Project) serverEntity).setApisHandler(apisHandler);
                } else {
                    logger.warn("Unsupported type {} when deserializing {}", type, json);
                }

                if (serverEntity != null) {
                    Owner owner = context.deserialize(((JsonObject) json).get("omero:details").getAsJsonObject().get("owner"), Owner.class);
                    if (owner != null) {
                        serverEntity.owner = owner;
                    }

                    Group group = context.deserialize(((JsonObject) json).get("omero:details").getAsJsonObject().get("group"), Group.class);
                    if (group != null) {
                        serverEntity.group = group;
                    }
                }

                return serverEntity;
            } catch (Exception e) {
                logger.error("Could not deserialize " + json, e);
                return null;
            }
        }
    }

    private boolean filterProjectsByName(String filter) {
        return filter == null || filter.isEmpty() || !(this instanceof Project) || getName().toLowerCase().contains(filter.toLowerCase());
    }
}
