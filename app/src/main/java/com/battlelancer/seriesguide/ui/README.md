# Changing names and package

Activities that are referenced from the AndroidManifest.xml or are used via `shortcuts.xml`
**must keep their package name and name**. This is to avoid shortcuts from breaking 
(e.g. launcher shortcuts, user created shortcuts, etc.).
