/**
 * <p>This package contains classes describing OMERO annotations.</p>
 * <p>
 *     An OMERO annotation is <b>not</b> similar to a QuPath annotation.
 *     It represents metadata attached to OMERO entities.
 * </p>
 * <p>An annotation is represented by the {@link qupath.lib.images.servers.omero.common.omero_entities.annotations.Annotation Annotation} abstract class.</p>
 * <p>
 *     The {@link qupath.lib.images.servers.omero.common.omero_entities.annotations.annotations annotations} package
 *     contains subclasses of Annotation.
 * </p>
 * <p>
 *     An OMERO entity can have multiple annotations attached to it. Therefore, they can be grouped in an
 *     {@link qupath.lib.images.servers.omero.common.omero_entities.annotations.AnnotationGroup AnnotationGroup}.
 * </p>
 * <p>
 *     An annotation can also contains complex data types, which are described in the
 *     {@link qupath.lib.images.servers.omero.common.omero_entities.annotations.entities entities} package.
 * </p>
 */
package qupath.lib.images.servers.omero.common.omero_entities.annotations;