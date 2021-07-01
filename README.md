# QuPath OMERO extension

Welcome to the OMERO extension for [QuPath](http://qupath.github.io)!

This adds support for accessing images hosted on an [OMERO](https://www.openmicroscopy.org/omero/) 
server through OMERO's web API.

> **Important!**
> 
> The use of the web API means that all images are 
JPEG-compressed.
This effectively means it is most useful for viewing and annotating RGB images 
(including whole slide images), but is not suitable for quantitative analysis 
where JPEG compression artifacts would be problematic.

The extension is intended for the (at the time of writing) not-yet-released 
QuPath v0.3.
It is not compatible with earlier QuPath versions.


## Building

You can build the extension using OpenJDK 11 or later with

```bash
gradlew clean build
```

The output will be under `build/libs`.
You can drag the jar file on top of QuPath to install the extension.
