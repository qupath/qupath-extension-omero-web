[![Extension docs](https://img.shields.io/badge/docs-qupath_omero-red)](https://qupath.readthedocs.io/en/stable/docs/advanced/omero.html)
[![Forum](https://img.shields.io/badge/forum-image.sc-green)](https://forum.image.sc/tag/qupath)
[![Downloads (latest release)](https://img.shields.io/github/downloads-pre/qupath/qupath-extension-omero/latest/total)](https://github.com/qupath/qupath-extension-omero/releases/latest)
[![Downloads (all releases)](https://img.shields.io/github/downloads/qupath/qupath-extension-omero/total)](https://github.com/qupath/qupath-extension-omero/releases)

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

## Installing

To install the OMERO extension, download the latest `qupath-extension-omero-[version].jar` file from [releases](https://github.com/qupath/qupath-extension-omero/releases) and drag it onto the main QuPath window.

If you haven't installed any extensions before, you'll be prompted to select a QuPath user directory.
The extension will then be copied to a location inside that directory.

You might then need to restart QuPath (but not your computer).


## Building

You can build the extension using OpenJDK 11 or later with

```bash
gradlew clean build
```

The output will be under `build/libs`.
You can drag the jar file on top of QuPath to install the extension.
