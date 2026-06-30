-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson models persisted to JSON
-keepattributes Signature
-keep class io.github.patricksmill.quicknotes.model.note.Note { *; }
-keep class io.github.patricksmill.quicknotes.model.tag.Tag { *; }
