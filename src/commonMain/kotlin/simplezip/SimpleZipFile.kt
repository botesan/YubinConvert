package simplezip

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import simplezip.crc32.calcCRC32
import simplezip.deflate.Deflate
import kotlin.time.Duration.Companion.microseconds

// https://pkwaredownloads.blob.core.windows.net/pkware-general/Documentation/APPNOTE_6.2.0.TXT

private const val DEBUG_LOG = false
private inline fun debugLog(message: () -> String) {
    if (DEBUG_LOG) println(message())
}

class SimpleZipFile(private val data: ByteArray) {
    private val deflate: Deflate = Deflate()
    private val eocdr: ZipEOCDR = readEndOfCentralDirectoryRecord(data).also {
        check(value = it.numberOfThisDisk == 0.toUShort()) { "Multi-disk zip file is not supported" }
        check(value = it.numberOfTheDiskWithStartOfCentralDirectory == 0.toUShort()) { "Multi-disk zip file is not supported" }
        check(value = it.totalEntriesOnThisDisk == it.totalEntries) { "Multi-disk zip file is not supported" }
    }
    private val entries: List<Entry> by lazy {
        readCentralDirectoryHeaders(data, eocdr).also {
            check(value = it.size == eocdr.totalEntries.toInt()) { "Invalid number of central directory headers" }
            check(value = it.all { cdh -> cdh.diskNumberStart == 0.toUShort() }) { "Multi-disk zip file is not supported" }
        }.map { cdh -> Entry(this, cdh) }
    }

    fun entries(): List<Entry> = entries

    class Entry(val zipFile: SimpleZipFile, private val cdh: ZipCDH) {
        val path: String
            get() = ByteString(data = cdh.fileName).decodeToString()
        val size: UInt
            get() = cdh.uncompressedSize
        val lastModified: Instant
            get() = cdh.ntfsLastModified ?: cdh.lastModified.toInstant(TimeZone.currentSystemDefault())

        fun uncompress(): ByteArray {
            debugLog { path }
            debugLog { cdh.toString() }
            check(value = (cdh.versionMadeBy and 0x00FFu) >= cdh.versionNeededToExtract) {
                "Invalid version made by. ${cdh.versionMadeBy}"
            }
            check(value = cdh.versionNeededToExtract <= 20u) {
                "Not support version needed to extract. ${cdh.versionNeededToExtract}"
            }
            check(value = cdh.generalPurposeBitFlag and 0x0001u == 0.toUShort()) {
                val flag = cdh.generalPurposeBitFlag.toString(radix = 2).padStart(length = 16, padChar = '0')
                "Not support general purpose bit flag. $flag"
            }
            val lfh = zipFile.readLocalFileHeader(zipFile.data, zipFile.eocdr, cdh)
            debugLog { lfh.toString() }
            debugLog { cdh.lastModified.toString() }
            debugLog { cdh.lastModified.toInstant(TimeZone.currentSystemDefault()).toString() }
            debugLog { cdh.ntfsLastModified.toString() }
            val startIndex = zipFile.data.size - zipFile.eocdr.size - zipFile.eocdr.sizeOfCentralDirectory.toInt() -
                    zipFile.eocdr.offsetOfStartOfCentralDirectory.toInt() + cdh.relativeOffsetOfLocalHeader.toInt() +
                    lfh.size
            val endIndex = startIndex + cdh.compressedSize.toInt()
            debugLog { "startIndex=$startIndex, endIndex=$endIndex, compressedSize=${cdh.compressedSize}" }
            val uncompressed: ByteArray
            when (cdh.compressionMethod) {
                // uncompressed
                0.toUShort() -> {
                    debugLog { "Uncompressed data." }
                    uncompressed = zipFile.data.sliceArray(indices = startIndex..<endIndex)
                }
                // deflate
                8.toUShort() -> {
                    debugLog { "Deflate data." }
                    val from = Buffer()
                        .also { it.write(source = zipFile.data, startIndex = startIndex, endIndex = endIndex) }
                    val to = Buffer()
                    zipFile.deflate.uncompressDeflate(from = from, to = to, maxWriteSize = cdh.uncompressedSize.toInt())
                    uncompressed = to.readByteArray()
                    check(value = uncompressed.size == cdh.uncompressedSize.toInt()) {
                        "Invalid uncompressed size. ${uncompressed.size} != ${cdh.uncompressedSize}"
                    }
                }
                // error
                else -> error("Unknown compression method: ${cdh.compressionMethod}")
            }
            val crc32 = calcCRC32(data = uncompressed)
            check(value = crc32 == cdh.crc32) {
                "Invalid crc32. ${crc32.toString(radix = 16)} != ${cdh.crc32.toString(radix = 16)}"
            }
            return uncompressed
        }
    }

    private fun readLocalFileHeader(data: ByteArray, eocdr: ZipEOCDR, cdh: ZipCDH): ZipLFH {
        val startIndex = data.size - eocdr.size - eocdr.sizeOfCentralDirectory.toInt() -
                eocdr.offsetOfStartOfCentralDirectory.toInt() + cdh.relativeOffsetOfLocalHeader.toInt()
        val endIndex = startIndex + ZipLFH.MIN_SIZE
        val buffer = Buffer().also { it.write(data, startIndex = startIndex, endIndex = endIndex) }
        // local file header signature
        val localFileHeaderSignature = buffer.readUIntLe()
        // version needed to extract
        val versionNeededToExtract = buffer.readUShortLe()
        // general purpose bit flag
        val generalPurposeBitFlag = buffer.readUShortLe()
        // compression method
        val compressionMethod = buffer.readUShortLe()
        // last mod file time
        val lastModFileTime = buffer.readUShortLe()
        // last mod file date
        val lastModFileDate = buffer.readUShortLe()
        // crc-32
        val crc32 = buffer.readUIntLe()
        // compressed size
        val compressedSize = buffer.readUIntLe()
        // uncompressed size
        val uncompressedSize = buffer.readUIntLe()
        // file name length
        val fileNameLength = buffer.readUShortLe()
        // extra field length
        val extraFieldLength = buffer.readUShortLe()
        check(value = buffer.exhausted()) { "Illegal local file header" }
        buffer.write(
            data,
            startIndex = endIndex,
            endIndex = endIndex + fileNameLength.toInt() + extraFieldLength.toInt()
        )
        // file name
        val fileName = buffer.readByteArray(fileNameLength.toInt())
        // extra field
        val extraField = buffer.readByteArray(extraFieldLength.toInt())
        return ZipLFH(
            signature = localFileHeaderSignature,
            versionNeededToExtract = versionNeededToExtract,
            generalPurposeBitFlag = generalPurposeBitFlag,
            compressionMethod = compressionMethod,
            lastModFileTime = lastModFileTime,
            lastModFileDate = lastModFileDate,
            crc32 = crc32,
            compressedSize = compressedSize,
            uncompressedSize = uncompressedSize,
            fileNameLength = fileNameLength,
            extraFieldLength = extraFieldLength,
            fileName = fileName,
            extraField = extraField,
        )
    }

    private fun readCentralDirectoryHeaders(data: ByteArray, eocdr: ZipEOCDR): List<ZipCDH> {
        val endIndex = data.size - eocdr.size
        val startIndex = endIndex - eocdr.sizeOfCentralDirectory.toInt()
        val buffer = Buffer().also { it.write(data, startIndex = startIndex, endIndex = endIndex) }
        return (0 until eocdr.totalEntries.toInt()).map {
            // central file header signature
            val centralFileHeaderSignature = buffer.readUIntLe()
            // version made by
            val versionMadeBy = buffer.readUShortLe()
            // version needed to extract
            val versionNeededToExtract = buffer.readUShortLe()
            // general purpose bit flag
            val generalPurposeBitFlag = buffer.readUShortLe()
            // compression method
            val compressionMethod = buffer.readUShortLe()
            // last mod file time
            val lastModFileTime = buffer.readUShortLe()
            // last mod file date
            val lastModFileDate = buffer.readUShortLe()
            // crc-32
            val crc32 = buffer.readUIntLe()
            // compressed size
            val compressedSize = buffer.readUIntLe()
            // uncompressed size
            val uncompressedSize = buffer.readUIntLe()
            // file name length
            val fileNameLength = buffer.readUShortLe()
            // extra field length
            val extraFieldLength = buffer.readUShortLe()
            // file comment length
            val fileCommentLength = buffer.readUShortLe()
            // disk number start
            val diskNumberStart = buffer.readUShortLe()
            // internal file attributes
            val internalFileAttributes = buffer.readUShortLe()
            // external file attributes
            val externalFileAttributes = buffer.readUIntLe()
            // relative offset of local header
            val relativeOffsetOfLocalHeader = buffer.readUIntLe()
            // file name
            val fileName = buffer.readByteArray(fileNameLength.toInt())
            // extra field
            val extraField = buffer.readByteArray(extraFieldLength.toInt())
            // file comment
            val fileComment = buffer.readByteArray(fileCommentLength.toInt())
            ZipCDH(
                signature = centralFileHeaderSignature,
                versionMadeBy = versionMadeBy,
                versionNeededToExtract = versionNeededToExtract,
                generalPurposeBitFlag = generalPurposeBitFlag,
                compressionMethod = compressionMethod,
                lastModFileTime = lastModFileTime,
                lastModFileDate = lastModFileDate,
                crc32 = crc32,
                compressedSize = compressedSize,
                uncompressedSize = uncompressedSize,
                fileNameLength = fileNameLength,
                extraFieldLength = extraFieldLength,
                fileCommentLength = fileCommentLength,
                diskNumberStart = diskNumberStart,
                internalFileAttributes = internalFileAttributes,
                externalFileAttributes = externalFileAttributes,
                relativeOffsetOfLocalHeader = relativeOffsetOfLocalHeader,
                fileName = fileName,
                extraField = extraField,
                fileComment = fileComment,
            )
        }
    }

    private fun readEndOfCentralDirectoryRecord(data: ByteArray): ZipEOCDR {
        val index = data.lastIndexOfEndOfCentralDirectoryRecord()
        check(value = index != -1) { "End of central directory header not found" }
        val buffer = Buffer().also { it.write(data, startIndex = index) }
        // end of central dir signature
        val endOfCentralDirectorySignature = buffer.readUIntLe()
        // number of this disk
        val numberOfThisDisk = buffer.readUShortLe()
        // number of the disk with the start of the central directory
        val numberOfTheDiskWithStartOfCentralDirectory = buffer.readUShortLe()
        // total number of entries in the central directory on this disk
        val totalEntriesOnThisDisk = buffer.readUShortLe()
        // total number of entries in the central directory
        val totalEntries = buffer.readUShortLe()
        // size of the central directory
        val sizeOfCentralDirectory = buffer.readUIntLe()
        // offset of start of central directory
        val offsetOfStartOfCentralDirectory = buffer.readUIntLe()
        // .ZIP file comment length
        val zipFileCommentLength = buffer.readUShortLe()
        // .ZIP file comment
        val zipFileComment = buffer.readByteArray(zipFileCommentLength.toInt())
        check(value = buffer.exhausted()) { "Illegal end of central directory header" }
        return ZipEOCDR(
            signature = endOfCentralDirectorySignature,
            numberOfThisDisk = numberOfThisDisk,
            numberOfTheDiskWithStartOfCentralDirectory = numberOfTheDiskWithStartOfCentralDirectory,
            totalEntriesOnThisDisk = totalEntriesOnThisDisk,
            totalEntries = totalEntries,
            sizeOfCentralDirectory = sizeOfCentralDirectory,
            offsetOfStartOfCentralDirectory = offsetOfStartOfCentralDirectory,
            zipFileCommentLength = zipFileCommentLength,
            zipFileComment = zipFileComment,
        )
    }

    private fun ByteArray.lastIndexOfEndOfCentralDirectoryRecord(lastIndex: Int = size - ZipEOCDR.MIN_SIZE): Int {
        val startIndex = (size - ZipEOCDR.MAX_SIZE).coerceAtLeast(minimumValue = 0)
        for (index in lastIndex downTo startIndex) {
            if (this[index] == ZipEOCDR.SIGNATURE_1 &&
                this[index + 1] == ZipEOCDR.SIGNATURE_2 &&
                this[index + 2] == ZipEOCDR.SIGNATURE_3 &&
                this[index + 3] == ZipEOCDR.SIGNATURE_4
            ) {
                return index
            }
        }
        return -1
    }

    private data class ZipLFH(
        val signature: UInt,
        val versionNeededToExtract: UShort,
        val generalPurposeBitFlag: UShort,
        val compressionMethod: UShort,
        val lastModFileTime: UShort,
        val lastModFileDate: UShort,
        val crc32: UInt,
        val compressedSize: UInt,
        val uncompressedSize: UInt,
        val fileNameLength: UShort,
        val extraFieldLength: UShort,
        val fileName: ByteArray,
        val extraField: ByteArray,
    ) {
        companion object {
            private const val SIGNATURE_1: Byte = 'P'.code.toByte()
            private const val SIGNATURE_2: Byte = 'K'.code.toByte()
            private const val SIGNATURE_3: Byte = 0x03.toByte()
            private const val SIGNATURE_4: Byte = 0x04.toByte()
            val SIGNATURE: UInt = SIGNATURE_1.toUInt() or
                    (SIGNATURE_2.toUInt() shl 8) or
                    (SIGNATURE_3.toUInt() shl 16) or
                    (SIGNATURE_4.toUInt() shl 24)
            const val MIN_SIZE: Int = 30
        }

        init {
            require(signature == SIGNATURE) { "Invalid local file header signature." }
            require(fileNameLength.toInt() == fileName.size) { "Invalid file name length" }
            require(extraFieldLength.toInt() == extraField.size) { "Invalid extra field length" }
        }

        val size: Int
            get() = MIN_SIZE + fileNameLength.toInt() + extraFieldLength.toInt()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ZipLFH) return false
            if (signature != other.signature) return false
            if (versionNeededToExtract != other.versionNeededToExtract) return false
            if (generalPurposeBitFlag != other.generalPurposeBitFlag) return false
            if (compressionMethod != other.compressionMethod) return false
            if (lastModFileTime != other.lastModFileTime) return false
            if (lastModFileDate != other.lastModFileDate) return false
            if (crc32 != other.crc32) return false
            if (compressedSize != other.compressedSize) return false
            if (uncompressedSize != other.uncompressedSize) return false
            if (fileNameLength != other.fileNameLength) return false
            if (extraFieldLength != other.extraFieldLength) return false
            if (!fileName.contentEquals(other.fileName)) return false
            if (!extraField.contentEquals(other.extraField)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = signature.hashCode()
            result = 31 * result + versionNeededToExtract.hashCode()
            result = 31 * result + generalPurposeBitFlag.hashCode()
            result = 31 * result + compressionMethod.hashCode()
            result = 31 * result + lastModFileTime.hashCode()
            result = 31 * result + lastModFileDate.hashCode()
            result = 31 * result + crc32.hashCode()
            result = 31 * result + compressedSize.hashCode()
            result = 31 * result + uncompressedSize.hashCode()
            result = 31 * result + fileNameLength.hashCode()
            result = 31 * result + extraFieldLength.hashCode()
            result = 31 * result + fileName.contentHashCode()
            result = 31 * result + extraField.contentHashCode()
            return result
        }
    }

    data class ZipCDH(
        val signature: UInt,
        val versionMadeBy: UShort,
        val versionNeededToExtract: UShort,
        val generalPurposeBitFlag: UShort,
        val compressionMethod: UShort,
        val lastModFileTime: UShort,
        val lastModFileDate: UShort,
        val crc32: UInt,
        val compressedSize: UInt,
        val uncompressedSize: UInt,
        val fileNameLength: UShort,
        val extraFieldLength: UShort,
        val fileCommentLength: UShort,
        val diskNumberStart: UShort,
        val internalFileAttributes: UShort,
        val externalFileAttributes: UInt,
        val relativeOffsetOfLocalHeader: UInt,
        val fileName: ByteArray,
        val extraField: ByteArray,
        val fileComment: ByteArray,
    ) {
        companion object {
            private const val SIGNATURE_1: Byte = 'P'.code.toByte()
            private const val SIGNATURE_2: Byte = 'K'.code.toByte()
            private const val SIGNATURE_3: Byte = 0x01.toByte()
            private const val SIGNATURE_4: Byte = 0x02.toByte()
            val SIGNATURE: UInt = SIGNATURE_1.toUInt() or
                    (SIGNATURE_2.toUInt() shl 8) or
                    (SIGNATURE_3.toUInt() shl 16) or
                    (SIGNATURE_4.toUInt() shl 24)
//            const val MIN_SIZE: Int = 46
        }

        init {
            require(value = signature == SIGNATURE) { "Invalid central directory header signature." }
            require(value = fileNameLength.toInt() == fileName.size) { "Invalid file name length" }
            require(value = extraFieldLength.toInt() == extraField.size) { "Invalid extra field length" }
            require(value = fileCommentLength.toInt() == fileComment.size) { "Invalid file comment length" }
        }

//        val size: Int
//            get() = MIN_SIZE + fileNameLength.toInt() + extraFieldLength.toInt() + fileCommentLength.toInt()

        val lastModified: LocalDateTime
            get() {
                // 31 - 25	year
                val year = lastModFileDate.toInt() shr 9
                // 24 - 21	month
                val month = (lastModFileDate.toInt() shr 5) and 0b0000_1111
                // 20 - 16	day
                val day = lastModFileDate.toInt() and 0b0000_00001_1111
                // 15 - 11	hour
                val hour = lastModFileTime.toInt() shr 11
                // 10 - 5	min
                val minute = (lastModFileTime.toInt() shr 5) and 0b0011_1111
                // 5 - 0	seconds / 2
                val second = (lastModFileTime.toInt() and 0b0001_1111) shl 1
                return LocalDateTime(
                    year = year + 1980,
                    monthNumber = month,
                    dayOfMonth = day,
                    hour = hour,
                    minute = minute,
                    second = second,
                )
            }

        val ntfsLastModified: Instant?
            get() {
                /*
      extra field: (Variable)

          This is for expansion.  If additional information
          needs to be stored for special needs or for specific
          platforms, it should be stored here.  Earlier versions
          of the software can then safely skip this file, and
          find the next file or header.  This field will be 0
          length in version 1.0.

          In order to allow different programs and different types
          of information to be stored in the 'extra' field in .ZIP
          files, the following structure should be used for all
          programs storing data in this field:

          header1+data1 + header2+data2 . . .

          Each header should consist of:

            Header ID - 2 bytes
            Data Size - 2 bytes

          Note: all fields stored in Intel low-byte/high-byte order.
             :
          The current Header ID mappings defined by PKWARE are:
             :
          0x000a        NTFS

         -NTFS Extra Field (0x000a):

          The following is the layout of the NTFS attributes
          "extra" block. (Note: At this time the Mtime, Atime
          and Ctime values may be used on any WIN32 system.)

          Note: all fields stored in Intel low-byte/high-byte order.

          Value      Size       Description
          -----      ----       -----------
  (NTFS)  0x000a     2 bytes    Tag for this "extra" block type
          TSize      2 bytes    Size of the total "extra" block
          Reserved   4 bytes    Reserved for future use
          Tag1       2 bytes    NTFS attribute tag value #1
          Size1      2 bytes    Size of attribute #1, in bytes
          (var.)     Size1      Attribute #1 data
          .
          .
          .
          TagN       2 bytes    NTFS attribute tag value #N
          SizeN      2 bytes    Size of attribute #N, in bytes
          (var.)     SizeN      Attribute #N data

          For NTFS, values for Tag1 through TagN are as follows:
          (currently only one set of attributes is defined for NTFS)

          Tag        Size       Description
          -----      ----       -----------
          0x0001     2 bytes    Tag for attribute #1
          Size1      2 bytes    Size of attribute #1, in bytes
          Mtime      8 bytes    File last modification time
          Atime      8 bytes    File last access time
          Ctime      8 bytes    File creation time
             */
                if (extraFieldLength != 36.toUShort()) return null
                val buffer = Buffer().also { it.write(extraField) }
                if (buffer.readUShortLe() != 0x000a.toUShort()) return null
                if (buffer.readUShortLe() != 32.toUShort()) return null
                buffer.skip(4)
                if (buffer.readUShortLe() != 0x0001.toUShort()) return null
                if (buffer.readUShortLe() != 24.toUShort()) return null
                val modifiedTimeUtc100NanosecondsFrom1601 = buffer.readLongLe()
                val modifiedTimeUtcMillisecondsFrom1601 =
                    (modifiedTimeUtc100NanosecondsFrom1601 / 10).microseconds.inWholeMilliseconds
                val modifiedTimeUtcMillisecondsFrom1970 =
                    (LocalDateTime(year = 1601, monthNumber = 1, dayOfMonth = 1, hour = 0, minute = 0, second = 0)
                        .toInstant(TimeZone.UTC) -
                            LocalDateTime(
                                year = 1970,
                                monthNumber = 1,
                                dayOfMonth = 1,
                                hour = 0,
                                minute = 0,
                                second = 0
                            )
                                .toInstant(TimeZone.UTC))
                        .inWholeMilliseconds + modifiedTimeUtcMillisecondsFrom1601
                return Instant.fromEpochMilliseconds(epochMilliseconds = modifiedTimeUtcMillisecondsFrom1970)
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ZipCDH) return false
            if (signature != other.signature) return false
            if (versionMadeBy != other.versionMadeBy) return false
            if (versionNeededToExtract != other.versionNeededToExtract) return false
            if (generalPurposeBitFlag != other.generalPurposeBitFlag) return false
            if (compressionMethod != other.compressionMethod) return false
            if (lastModFileTime != other.lastModFileTime) return false
            if (lastModFileDate != other.lastModFileDate) return false
            if (crc32 != other.crc32) return false
            if (compressedSize != other.compressedSize) return false
            if (uncompressedSize != other.uncompressedSize) return false
            if (fileNameLength != other.fileNameLength) return false
            if (extraFieldLength != other.extraFieldLength) return false
            if (fileCommentLength != other.fileCommentLength) return false
            if (diskNumberStart != other.diskNumberStart) return false
            if (internalFileAttributes != other.internalFileAttributes) return false
            if (externalFileAttributes != other.externalFileAttributes) return false
            if (relativeOffsetOfLocalHeader != other.relativeOffsetOfLocalHeader) return false
            if (!fileName.contentEquals(other.fileName)) return false
            if (!extraField.contentEquals(other.extraField)) return false
            if (!fileComment.contentEquals(other.fileComment)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = signature.hashCode()
            result = 31 * result + versionMadeBy.hashCode()
            result = 31 * result + versionNeededToExtract.hashCode()
            result = 31 * result + generalPurposeBitFlag.hashCode()
            result = 31 * result + compressionMethod.hashCode()
            result = 31 * result + lastModFileTime.hashCode()
            result = 31 * result + lastModFileDate.hashCode()
            result = 31 * result + crc32.hashCode()
            result = 31 * result + compressedSize.hashCode()
            result = 31 * result + uncompressedSize.hashCode()
            result = 31 * result + fileNameLength.hashCode()
            result = 31 * result + extraFieldLength.hashCode()
            result = 31 * result + fileCommentLength.hashCode()
            result = 31 * result + diskNumberStart.hashCode()
            result = 31 * result + internalFileAttributes.hashCode()
            result = 31 * result + externalFileAttributes.hashCode()
            result = 31 * result + relativeOffsetOfLocalHeader.hashCode()
            result = 31 * result + fileName.contentHashCode()
            result = 31 * result + extraField.contentHashCode()
            result = 31 * result + fileComment.contentHashCode()
            return result
        }
    }

    private data class ZipEOCDR(
        val signature: UInt,
        val numberOfThisDisk: UShort,
        val numberOfTheDiskWithStartOfCentralDirectory: UShort,
        val totalEntriesOnThisDisk: UShort,
        val totalEntries: UShort,
        val sizeOfCentralDirectory: UInt,
        val offsetOfStartOfCentralDirectory: UInt,
        val zipFileCommentLength: UShort,
        val zipFileComment: ByteArray,
    ) {
        companion object {
            const val SIGNATURE_1: Byte = 'P'.code.toByte()
            const val SIGNATURE_2: Byte = 'K'.code.toByte()
            const val SIGNATURE_3: Byte = 0x05.toByte()
            const val SIGNATURE_4: Byte = 0x06.toByte()
            val SIGNATURE: UInt = SIGNATURE_1.toUInt() or
                    (SIGNATURE_2.toUInt() shl 8) or
                    (SIGNATURE_3.toUInt() shl 16) or
                    (SIGNATURE_4.toUInt() shl 24)
            const val MIN_SIZE: Int = 22
            const val MAX_SIZE: Int = MIN_SIZE + 0xFFFF
        }

        init {
            require(signature == SIGNATURE) { "Invalid end of central directory signature." }
            require(zipFileCommentLength.toInt() == zipFileComment.size) { "Invalid zip file comment length" }
        }

        val size: Int
            get() = MIN_SIZE + zipFileCommentLength.toInt()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ZipEOCDR) return false
            if (signature != other.signature) return false
            if (numberOfThisDisk != other.numberOfThisDisk) return false
            if (numberOfTheDiskWithStartOfCentralDirectory != other.numberOfTheDiskWithStartOfCentralDirectory) return false
            if (totalEntriesOnThisDisk != other.totalEntriesOnThisDisk) return false
            if (totalEntries != other.totalEntries) return false
            if (sizeOfCentralDirectory != other.sizeOfCentralDirectory) return false
            if (offsetOfStartOfCentralDirectory != other.offsetOfStartOfCentralDirectory) return false
            if (zipFileCommentLength != other.zipFileCommentLength) return false
            if (!zipFileComment.contentEquals(other.zipFileComment)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = signature.hashCode()
            result = 31 * result + numberOfThisDisk.hashCode()
            result = 31 * result + numberOfTheDiskWithStartOfCentralDirectory.hashCode()
            result = 31 * result + totalEntriesOnThisDisk.hashCode()
            result = 31 * result + totalEntries.hashCode()
            result = 31 * result + sizeOfCentralDirectory.hashCode()
            result = 31 * result + offsetOfStartOfCentralDirectory.hashCode()
            result = 31 * result + zipFileCommentLength.hashCode()
            result = 31 * result + zipFileComment.contentHashCode()
            return result
        }
    }
}
