package qupath.ext.omero.core.entities.repositoryentities.serverentities;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A server entity represents an OMERO entity belonging to the project/dataset/image
 * or the screen/plate/plate acquisition/well hierarchy.
 */
public abstract class ServerEntity implements RepositoryEntity {

    private static final Logger logger = LoggerFactory.getLogger(ServerEntity.class);
    @SerializedName(value = "@id") protected long id;
    @SerializedName(value = "Name") protected String name;
    private Owner owner = Owner.getAllMembersOwner();
    private Group group = Group.getAllGroupsGroup();

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof ServerEntity serverEntity))
            return false;
        return serverEntity.id == this.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean isFilteredByGroupOwnerName(Group groupFilter, Owner ownerFilter, String nameFilter) {
        return (groupFilter == null || groupFilter == Group.getAllGroupsGroup() || group.equals(groupFilter)) &&
                (ownerFilter == null || ownerFilter == Owner.getAllMembersOwner() || owner.equals(ownerFilter)) &&
                (nameFilter == null || nameFilter.isEmpty() || getLabel().get().toLowerCase().contains(nameFilter.toLowerCase()));
    }

    /**
     * Returns the <b>name</b> of an attribute associated with this entity.
     *
     * @param informationIndex the index of the attribute
     * @return the attribute name corresponding to the index, or an empty String if the index is out of bound
     */
    abstract public String getAttributeName(int informationIndex);

    /**
     * Returns the <b>value</b> of an attribute associated with this entity.
     *
     * @param informationIndex the index of the attribute
     * @return the attribute value corresponding to the index, or an empty String if the index is out of bound
     */
    abstract public String getAttributeValue(int informationIndex);

    /**
     * @return the total number of attributes this entity has
     */
    abstract public int getNumberOfAttributes();

    /**
     * Creates a stream of entities from a list of JSON elements.
     * If an entity cannot be created from a JSON element, it is discarded.
     *
     * @param jsonElements  the JSON elements supposed to represent server entities
     * @param client the corresponding web client
     * @return a stream of server entities
     */
    public static Stream<ServerEntity> createFromJsonElements(List<JsonElement> jsonElements, WebClient client) {
        return jsonElements.stream()
                .map(jsonElement -> createFromJsonElement(jsonElement, client))
                .flatMap(Optional::stream);
    }

    /**
     * Creates a server entity from a JSON element.
     *
     * @param jsonElement  the JSON element supposed to represent a server entity
     * @param client the corresponding web client
     * @return a server entity, or an empty Optional if it was impossible to create
     */
    public static Optional<ServerEntity> createFromJsonElement(JsonElement jsonElement, WebClient client) {
        Gson deserializer = new GsonBuilder().registerTypeAdapter(ServerEntity.class, new ServerEntityDeserializer(client)).setLenient().create();

        try {
            return Optional.ofNullable(deserializer.fromJson(jsonElement, ServerEntity.class));
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

    private record ServerEntityDeserializer(WebClient client) implements JsonDeserializer<ServerEntity> {
        @Override
        public ServerEntity deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                String type = json.getAsJsonObject().get("@type").getAsString().toLowerCase();

                ServerEntity serverEntity = null;
                if (Image.isImage(type)) {
                    serverEntity = context.deserialize(json, Image.class);
                    ((Image) serverEntity).setWebClient(client);
                } else if (Dataset.isDataset(type)) {
                    serverEntity = context.deserialize(json, Dataset.class);
                    ((Dataset) serverEntity).setApisHandler(client.getApisHandler());
                } else if (Project.isProject(type)) {
                    serverEntity = context.deserialize(json, Project.class);
                    ((Project) serverEntity).setApisHandler(client.getApisHandler());
                } else if (Screen.isScreen(type)) {
                    serverEntity = context.deserialize(json, Screen.class);
                    ((Screen) serverEntity).setApisHandler(client.getApisHandler());
                } else if (Plate.isPlate(type)) {
                    serverEntity = context.deserialize(json, Plate.class);
                    ((Plate) serverEntity).setApisHandler(client.getApisHandler());
                } else if (PlateAcquisition.isPlateAcquisition(type)) {
                    serverEntity = context.deserialize(json, PlateAcquisition.class);
                    ((PlateAcquisition) serverEntity).setApisHandler(client.getApisHandler());
                } else if (Well.isWell(type)) {
                    serverEntity = context.deserialize(json, Well.class);
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
}
