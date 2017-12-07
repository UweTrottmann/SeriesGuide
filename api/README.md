SeriesGuide Extensions API
==========================

See https://seriesgui.de/api/ for how to get started.

## Building

To build a new API release, update version number and target SDK.

Use 'uploadArchives' to publish to staging on Sonatype. Then 'closeAndReleaseRepository' to close 
the staging repository and promote/release it and its artifacts.

Use 'generateJavadoc' to generate docs for the website.

Building a jar is no longer possible, as the library is an AAR which can only be added via a dependency.