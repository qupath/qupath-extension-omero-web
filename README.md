# QuPath OMERO extension

Welcome to the OMERO extension for [QuPath](http://qupath.github.io)!

This adds support for accessing images hosted on an [OMERO](https://www.openmicroscopy.org/omero/) 
server through OMERO's web (and other) API.

> **Important!**
> 
> By default, this extension uses the OMERO **web** API to read images, which 
> has several limitations.
> See the [Reading images](#reading-images) section.

The extension is intended for QuPath v0.5 and later.
It is not compatible with earlier QuPath versions.

## Installing

To install the OMERO extension, download the latest `qupath-extension-omero-[version].jar` file from [releases](https://github.com/qupath/qupath-extension-omero/releases) and drag it onto the main QuPath window.

If you haven't installed any extensions before, you'll be prompted to select a QuPath user directory.
The extension will then be copied to a location inside that directory.

You might then need to restart QuPath (but not your computer).

## Reading images
The extension uses two APIs to read images:
* The **OMERO web API**. This method is enabled by default and available
on every OMERO server. It is fast but only 8-bit RGB images
can be read, and they are JPEG-compressed. This effectively means it is most useful
for viewing and annotating RGB images (including whole slide images), but is not
suitable for quantitative analysis where JPEG compression artifacts would be problematic.
* The **OMERO Ice API**. It can read every image and access raw pixel values. However,
you have to install the OMERO Java dependencies to enable it: from the
[OMERO download page](https://www.openmicroscopy.org/omero/downloads/), under
"OMERO Java", download the .zip file, unzip it and copy the *libs* folder in
your extension directory. Note that it is not possible to use the Ice API
when accessing an OMERO server with a guest account, you have to be 
authenticated.

## Building

You can build the extension using OpenJDK 17 or later with

```bash
gradlew clean build
```

The output will be under `build/libs`.
You can drag the jar file on top of QuPath to install the extension.
