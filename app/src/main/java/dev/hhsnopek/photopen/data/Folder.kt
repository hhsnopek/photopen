package dev.hhsnopek.photopen.data

/**
 * A folder as MediaStore calls it: one bucket. The id disambiguates two folders
 * with the same display name on different volumes.
 */
data class Folder(
    val key: String,
    val name: String,
    val count: Int,
)

/** Stable identity for the folder an item lives in. */
val MediaItem.folderKey: String
    get() = if (bucketId.isBlank()) "name:$bucketName" else "id:$bucketId"

/** Folders present in [items], alphabetical, with how many items each holds. */
fun foldersOf(items: List<MediaItem>): List<Folder> =
    items
        .groupBy { it.folderKey }
        .map { (key, group) ->
            Folder(
                key = key,
                name = group.first().bucketName.ifBlank { UNTITLED },
                count = group.size,
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

/** [items] narrowed to one folder, or all of them when [folderKey] is null. */
fun filterByFolder(
    items: List<MediaItem>,
    folderKey: String?,
): List<MediaItem> = if (folderKey == null) items else items.filter { it.folderKey == folderKey }

const val UNTITLED = "Untitled folder"
