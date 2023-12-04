#!/bin/sh

/opt/omero/server/venv3/bin/omero login root@localhost:4064 -w password

/opt/omero/server/venv3/bin/omero group add public-data --type=read-only
/opt/omero/server/venv3/bin/omero user add public public access --group-name public-data -P password

/opt/omero/server/venv3/bin/omero login public@localhost:4064 -w password

project=$(/opt/omero/server/venv3/bin/omero obj new Project name=project)
dataset=$(/opt/omero/server/venv3/bin/omero obj new Dataset name=dataset)
/opt/omero/server/venv3/bin/omero obj new ProjectDatasetLink parent=$project child=$dataset

comment=$(/opt/omero/server/venv3/bin/omero obj new CommentAnnotation textValue=comment)
/opt/omero/server/venv3/bin/omero obj new DatasetAnnotationLink parent=$dataset child=$comment

analysis=$(/opt/omero/server/venv3/bin/omero upload /resources/analysis.csv)
file=$(/opt/omero/server/venv3/bin/omero obj new FileAnnotation file=$analysis)
/opt/omero/server/venv3/bin/omero obj new DatasetAnnotationLink parent=$dataset child=$file

/opt/omero/server/venv3/bin/omero obj new Dataset name=orphaned_dataset

/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/rgb.tiff
/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/uint8.tiff
/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/int8.tiff
/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/uint16.tiff
/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/int16.tiff
/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/uint32.tiff
/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/int32.tiff
/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/float32.tiff
/opt/omero/server/venv3/bin/omero import -d $dataset /resources/images/float64.tiff
/opt/omero/server/venv3/bin/omero import /resources/images/complex.tiff

screen=$(/opt/omero/server/venv3/bin/omero obj new Screen name=screen)
/opt/omero/server/venv3/bin/omero import -r $screen /resources/plate/hcs.companion.ome

/opt/omero/server/venv3/bin/omero import /resources/plate/hcs.companion.ome

tar -czvf /tmp/OMERO.tar.gz /OMERO

echo $analysis